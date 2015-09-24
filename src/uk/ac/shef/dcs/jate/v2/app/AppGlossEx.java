package uk.ac.shef.dcs.jate.v2.app;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.v2.JATEProperties;
import uk.ac.shef.dcs.jate.v2.algorithm.CValue;
import uk.ac.shef.dcs.jate.v2.algorithm.GlossEx;
import uk.ac.shef.dcs.jate.v2.algorithm.TermInfoCollector;
import uk.ac.shef.dcs.jate.v2.feature.*;
import uk.ac.shef.dcs.jate.v2.model.JATETerm;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 */
public class AppGlossEx extends App {
    public static void main(String[] args) throws JATEException, IOException {
        if (args.length < 1) {
            printHelp();
            System.exit(1);
        }
        String indexPath = args[args.length - 2];
        Map<String, String> params = getParams(args);

        IndexReader indexReader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
        JATEProperties properties = new JATEProperties(args[args.length - 1]);
        FrequencyTermBasedFBMaster ftbb = new
                FrequencyTermBasedFBMaster(indexReader, properties, 0);
        FrequencyTermBased ftb = (FrequencyTermBased)ftbb.build();

        FrequencyTermBasedFBMaster fwbb = new
                FrequencyTermBasedFBMaster(indexReader, properties, 1);
        FrequencyTermBased fwb = (FrequencyTermBased)fwbb.build();

        TTFReferenceFeatureFileBuilder ftrb = new
                TTFReferenceFeatureFileBuilder(args[args.length-3]);
        FrequencyTermBased frb = ftrb.build();

        GlossEx glossex = new GlossEx();
        glossex.registerFeature(FrequencyTermBased.class.getName(), ftb);
        glossex.registerFeature(FrequencyTermBased.class.getName()+GlossEx.SUFFIX_WORD, fwb);
        glossex.registerFeature(FrequencyTermBased.class.getName()+GlossEx.SUFFIX_REF, frb);

        String paramValue=params.get("-c");
        if(paramValue!=null &&paramValue.equalsIgnoreCase("true"))
            glossex.setTermInfoCollector(new TermInfoCollector(indexReader));
        List<JATETerm> terms=glossex.execute(ftb.getMapTerm2TTF().keySet());
        terms=applyThresholds(terms, params.get("-t"), params.get("-n"));
        paramValue=params.get("-o");
        write(terms,paramValue);
        indexReader.close();
    }

    protected static void printHelp() {
        StringBuilder sb = new StringBuilder("GlossEx Usage:\n");
        sb.append("java -cp '[CLASSPATH]' ").append(AppATTF.class.getName())
                .append(" [OPTIONS] ").append("[REF_TERM_TF_FILE] [LUCENE_INDEX_PATH] [JATE_PROPERTY_FILE]").append("\nE.g.:\n");
        sb.append("java -cp '/libs/*' -t 20 /resource/bnc_unifrqs.normal /solr/server/solr/jate/data jate.properties ...\n\n");
        sb.append("[OPTIONS]:\n")
                .append("\t\t-c\t\t'true' or 'false'. Whether to collect term information, e.g., offsets in documents. Default is false.\n")
                .append("\t\t-t\t\tA number. Score threshold for selecting terms. If not set then default -n is used.").append("\n")
                .append("\t\t-n\t\tA number. If an integer is given, top N candidates are selected as terms. \n")
                .append("\t\t\t\tIf a decimal number is given, top N% of candidates are selected. Default is 0.25.\n");
        sb.append("\t\t-o\t\tA file path. If provided, the output is written to the file. \n")
                .append("\t\t\t\tOtherwise, output is written to the console.");
        System.out.println(sb);
    }
}
