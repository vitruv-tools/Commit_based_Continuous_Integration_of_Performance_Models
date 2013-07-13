package org.splevo.vpm.analyzer.semantic.lucene.finder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.splevo.vpm.analyzer.semantic.Constants;
import org.splevo.vpm.analyzer.semantic.StructuredMap;

/**
 * This is a special {@link AbstractRelationshipFinder} that allows similarity measurement
 * by querying the index.
 * 
 * @author Daniel Kojic
 *
 */
public abstract class AbstractLuceneQueryFinder extends AbstractRelationshipFinder {
	
	/**
	 * Initializations. Queries the content of the
	 * given {@link DirectoryReader}.
	 * 
	 * @param reader The {@link DirectoryReader}.
	 * @param matchComments Indicates whether to include comments for analysis or not.
	 */
	public AbstractLuceneQueryFinder(DirectoryReader reader, boolean matchComments) {
		super(reader, matchComments);
	}

	/** The logger for this class. */
    private Logger logger = Logger.getLogger(AbstractLuceneQueryFinder.class);
    
	@Override
	public StructuredMap findSimilarEntries() {
		StructuredMap result = new StructuredMap();
		try {
			IndexSearcher indexSearcher = new IndexSearcher(reader);
						
			// Iterate over all documents (VariationPoints).
			for (int i = 0; i < reader.maxDoc(); i++) {
				Document doc = indexSearcher.doc(i);
				
				if (doc.getField(Constants.INDEX_CONTENT) != null) {
					Map<String, Integer> contentFrequencies = getTermFrequencies(i, Constants.INDEX_CONTENT);
					Query contentQuery = buildQuery(Constants.INDEX_CONTENT, contentFrequencies);
					int maxDoc = reader.maxDoc();
					ScoreDoc[] contentHits = executeQuery(indexSearcher, maxDoc, contentQuery);
					String explanation = getExplanation();
					addHitsToStructuredMap(indexSearcher, result, contentHits, doc, explanation, contentQuery);
				}				
				
				if (matchComments && doc.getField(Constants.INDEX_COMMENT) != null) {
					Map<String, Integer> commentFrequencies = getTermFrequencies(i, Constants.INDEX_COMMENT);
					Query commentQuery = buildQuery(Constants.INDEX_COMMENT, commentFrequencies);
					int maxDoc = reader.maxDoc();
					ScoreDoc[] commentHits = executeQuery(indexSearcher, maxDoc, commentQuery);
					String explanation = getExplanation();					
					addHitsToStructuredMap(indexSearcher, result, commentHits, doc, explanation, commentQuery);
				}				
			}
		} catch (IOException e) {
			logger.error("Failure while searching Lucene index.", e);
		}
		return result;
	}

	/**
	 * This Method builds the {@link Query} the Finder uses to
	 * search similarities.
	 * 
	 * @param fieldName The name of the field that should be searched.
	 * @param termFrequencies A {@link Map} that contains all terms and their frequencies.
	 * @return The {@link Query}.
	 */
	protected abstract Query buildQuery(String fieldName, Map<String, Integer> termFrequencies);
	
	/**
	 * Gets a explanation for the found results.
	 * 
	 * @return The text explanation.
	 */
	protected abstract String getExplanation();

	/**
	 * Adds given {@link ScoreDoc} to a {@link StructuredMap}.
	 * 
	 * @param indexSearcher The {@link IndexSearcher} to search the index with.
	 * @param result The {@link StructuredMap} to add the results to.
	 * @param hits The query hits to be added to the {@link StructuredMap}.
	 * @param doc The relevant document.
	 * @param explanation An explanation that explains the existence of the relationships.
	 * @param query The query that was searched for.
	 * @throws IOException If document doesn't exist in the given {@link IndexSearcher}.
	 */
	private void addHitsToStructuredMap(IndexSearcher indexSearcher,
			StructuredMap result, ScoreDoc[] hits, Document doc, String explanation, Query query)
			throws IOException {
		for (int q = 0; q < hits.length; q++) {					
			String id1 = doc.get(Constants.INDEX_VARIATIONPOINT);
			Document document = indexSearcher.doc(hits[q].doc);
			String id2 = document.get(Constants.INDEX_VARIATIONPOINT);

			Set<String> sharedTerms = determineSharedTerms(query, document);
			explanation = "Shared terms: " + sharedTerms;
			result.addLink(id1, id2, explanation);
		}
	}

    /**
     * Determine the terms shared by the related variation points
     * by looking up all terms included in the search query AND a found
     * document.
     * 
     * @param query The searched query.
     * @param document A specific document found by the query.
     * @return The {@link Set} of terms shared between the query and the document.
     */
    private Set<String> determineSharedTerms(Query query, Document document) {
        Set<String> sharedTerms = new TreeSet<String>();
        Set<Term> terms = new HashSet<Term>();
        query.extractTerms(terms);
        for (IndexableField field : document.getFields()) {
            if (field.stringValue() != null) {
                for (Term term : terms) {
                    if (field.stringValue().indexOf(term.text()) != -1) {
                        sharedTerms.add(term.text());
                    }
                }
            }
        }
        return sharedTerms;
    }
	
	/**
	 * Executes a query.
	 * 
	 * @param indexSearcher The {@link IndexSearcher} to be used.
	 * @param maxDoc The max. number of results.
	 * @param query The {@link Query} to be executed.
	 * @return The result of the search.
	 * @throws IOException If there were errors while executing the query.
	 */
	private ScoreDoc[] executeQuery(IndexSearcher indexSearcher, int maxDoc,
			Query query) throws IOException {
		TopScoreDocCollector collector = TopScoreDocCollector.create(maxDoc, true);
		indexSearcher.search(query, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;
		return hits;
	}
}
