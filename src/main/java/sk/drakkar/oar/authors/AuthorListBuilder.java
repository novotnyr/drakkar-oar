package sk.drakkar.oar.authors;

import java.io.File;
import java.io.IOException;
import java.text.Collator;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sk.drakkar.oar.*;

import com.google.common.base.Charsets;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder.ListMultimapBuilder;
import com.google.common.io.Files;
import sk.drakkar.oar.plugin.Plugin;

public class AuthorListBuilder implements Plugin {
	
	private static final Logger logger = LoggerFactory.getLogger(AuthorListBuilder.class);
	
	private Configuration configuration;

	private AuthorListTemplater authorListTemplater = new AuthorListTemplater();
	
	private Multimap<String, Article> authorMap;

	private boolean removingNicknameNames = true;
	
	public AuthorListBuilder(Configuration configuration) {
		this.configuration = configuration;
		
		authorMap = ListMultimapBuilder
			.treeKeys(getCaseInsensitiveCzechCollator())
			.arrayListValues()
			.build();		
	}
	
	private Collator getCaseInsensitiveCzechCollator() {
		Collator collator = Collator.getInstance(new Locale("cz"));
		collator.setStrength(Collator.SECONDARY);
		collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
		
		return collator;
	}
	
	private void write(String html) {
		try {
			File outputFile = new File(this.configuration.getOutputFolder(), "authors.html");
			Files.write(html, outputFile, Charsets.UTF_8);
		} catch (IOException e) {
			throw new AuthorListBuildingException("Unable to write author list", e);
		}
	}

	@Override
	public void issueArticlesProcessed(Issue issue) {
		logger.info("Building author list " + issue.getNumber());
		
		for (Article article : issue.getArticles()) {
			for(String author : article.getMetadata().getAuthors()) {
				String sanitizedAuthor = sanitizeAuthor(author);
				authorMap.put(sanitizedAuthor, article);
			}
		}
	}

	private String sanitizeAuthor(String author) {
		if(isRemovingNicknameNames()) {
			if(author.startsWith("„") && author.endsWith("“")) {
				return author.substring(1, author.length() - 1);
			}
		}
		return author;
	}

	@Override
	public void publicationComplete() {
		String html = authorListTemplater.convert(this.authorMap);
		write(html);
		
		logger.info("Written author list.");
	}

	/**
	 * Remove introductory quotes from authors with a nickname, e. g.
	 * <code>„Alhmar”</code> becomes <code>Alhmar</code>.
	 * <p>
	 *     This option is turned on by default.
	 * </p>
	 */
	public void setRemovingNicknameNames(boolean nicknamesQuoteRemoval) {
		this.removingNicknameNames = nicknamesQuoteRemoval;
	}

	public boolean isRemovingNicknameNames() {
		return removingNicknameNames;
	}
}
