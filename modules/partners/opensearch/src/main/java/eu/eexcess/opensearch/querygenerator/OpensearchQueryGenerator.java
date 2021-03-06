/**
 * Copyright (C) 2014
 * "Kompetenzzentrum fuer wissensbasierte Anwendungen Forschungs- und EntwicklungsgmbH" 
 * (Know-Center), Graz, Austria, office@know-center.at.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.eexcess.opensearch.querygenerator;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.CharEncoding;

import eu.eexcess.dataformats.userprofile.ContextKeyword;
import eu.eexcess.dataformats.userprofile.SecureUserProfile;
import eu.eexcess.opensearch.recommender.PartnerConnector;
import eu.eexcess.partnerrecommender.api.QueryGeneratorApi;

/**
 * Generate search terms for OpenSearch applying SecureUserProfile.
 * 
 * @author Raoul Rubien
 */
public class OpensearchQueryGenerator implements QueryGeneratorApi {

	private static final Logger mLogger = Logger.getLogger(PartnerConnector.class.getName());

	private static final String mDefaultUrlEncoding = CharEncoding.UTF_8;;

	/**
	 * Concatenate space separated keywords and search result limit if @param
	 * userProfile.numResults > 0
	 */
	@Override
	public String toQuery(SecureUserProfile userProfile) {

		StringBuilder stringBuilder = new StringBuilder();

		for (ContextKeyword keyword : userProfile.contextKeywords) {
			stringBuilder.append(keyword.text + " ");
		}

		if (stringBuilder.length() > 0) {
			stringBuilder.deleteCharAt(stringBuilder.length() - 1);
		}

		String urlEncodedKeywords = null;
		try {
			urlEncodedKeywords = URLEncoder.encode(stringBuilder.toString(), mDefaultUrlEncoding);

			if (userProfile.numResults != null && userProfile.numResults > 0) {
				return urlEncodedKeywords + "&limit=" + userProfile.numResults;
			}
			
		} catch (UnsupportedEncodingException e) {
			mLogger.log(Level.WARNING, "failed encoding keywords");
		}

		return urlEncodedKeywords;
	}

}
