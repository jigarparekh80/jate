package uk.ac.shef.dcs.jate.feature;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.solr.search.SolrIndexSearcher;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Logger;

/**
 * Created by - on 18/10/2015.
 */
public class FrequencyCtxWindowBasedFBMaster extends AbstractFeatureBuilder{
    private static final Logger LOG = Logger.getLogger(FrequencyCtxWindowBasedFBMaster.class.getName());

    private int termOrWord; //0 means term; 1 means word
    private int window;

    public FrequencyCtxWindowBasedFBMaster(SolrIndexSearcher solrIndexSearcher, JATEProperties properties,
                                           int window, int termOrWord) {
        super(solrIndexSearcher, properties);
        this.termOrWord=termOrWord;
        this.window=window;
    }

    @Override
    public AbstractFeature build() throws JATEException {
        FrequencyCtxBased feature = new FrequencyCtxBased();
        List<Integer> allDocs = new ArrayList<>();
        for (int i = 0; i < solrIndexSearcher.maxDoc(); i++) {
            allDocs.add(i);
        }

        try {
            Set<String> allCandidates;
            if(termOrWord==0)
                allCandidates=getUniqueTerms();
            else
                allCandidates=getUniqueWords();


            //start workers
            int cores = properties.getMaxCPUCores();
            cores = cores == 0 ? 1 : cores;
            int maxPerThread = allDocs.size()/cores;
            if(maxPerThread==0)
                maxPerThread=50;

            FrequencyCtxWindowBasedFBWorker worker = new
                    FrequencyCtxWindowBasedFBWorker(feature, properties, allDocs, allCandidates,
                    solrIndexSearcher, window, maxPerThread
            );
            StringBuilder sb = new StringBuilder("Building features using cpu cores=");
            sb.append(cores).append(", total docs=").append(allDocs.size()).append(", max per worker=")
                    .append(maxPerThread);
            LOG.info(sb.toString());
            ForkJoinPool forkJoinPool = new ForkJoinPool(cores);
            int total = forkJoinPool.invoke(worker);
            sb = new StringBuilder("Complete building features. Total sentence ctx=");
            sb.append(feature.getMapCtx2TTF().size()).append(", from total processed docs=").append(total);
            LOG.info(sb.toString());
        } catch (IOException ioe) {
            StringBuilder sb = new StringBuilder("Failed to build features!");
            sb.append("\n").append(ExceptionUtils.getFullStackTrace(ioe));
            LOG.severe(sb.toString());
            throw new JATEException(sb.toString());
        }
        return feature;
    }
}
