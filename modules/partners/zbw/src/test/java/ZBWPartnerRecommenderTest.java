/* Copyright (C) 2014
"JOANNEUM RESEARCH Forschungsgesellschaft mbH" 
 Graz, Austria, digital-iis@joanneum.at.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.BeforeClass;
import org.junit.Test;

import eu.eexcess.dataformats.result.ResultList;
import eu.eexcess.zbw.webservice.tool.PartnerStandaloneServer;
import eu.eexcess.partnerrecommender.test.PartnerRecommenderTestHelper;


public class ZBWPartnerRecommenderTest {

	private static int port = 8812;
	private static PartnerStandaloneServer server;
	
	public  ZBWPartnerRecommenderTest() {
		
	}
	
	@SuppressWarnings("static-access")
	@BeforeClass
    static public void startJetty() throws Exception {
		server = new PartnerStandaloneServer();
		server.start(port);
    }
	
	@Test
	public void singleQueryWomenWorkforceChina() {
		ArrayList<String> keywords = new ArrayList<String>();
		keywords.add("women workforce china");
		ResultList resultList = PartnerRecommenderTestHelper.getRecommendations("eexcess-partner-zbw-1.0-SNAPSHOT",	
				port, PartnerRecommenderTestHelper.createParamsForPartnerRecommender(20, keywords));
	    
        assertNotNull(resultList);
        assertTrue(resultList.results.size() > 0 );
        assertEquals(4, resultList.results.size());

	}

	@Test
	public void singleQueryFrauenarbeit() {
		ArrayList<String> keywords = new ArrayList<String>();
		keywords.add("frauenarbeit");
		ResultList resultList = PartnerRecommenderTestHelper.getRecommendations("eexcess-partner-zbw-1.0-SNAPSHOT",	
				port, PartnerRecommenderTestHelper.createParamsForPartnerRecommender(20, keywords));
	    
        assertNotNull(resultList);
        assertTrue(resultList.results.size() > 0 );
        assertEquals(20, resultList.results.size());

	}
	
	@Test
	public void singleQueryserendipity() {
		ArrayList<String> keywords = new ArrayList<String>();
		keywords.add("serendipity");
		ResultList resultList = PartnerRecommenderTestHelper.getRecommendations("eexcess-partner-zbw-1.0-SNAPSHOT",	
				port, PartnerRecommenderTestHelper.createParamsForPartnerRecommender(20, keywords));
	    
        assertNotNull(resultList);
        assertTrue(resultList.results.size() > 0 );
        assertEquals(20, resultList.results.size());

	}
	

	@Test
	public void singleQueryFrauenarbeitChinaArbeitsbedingungenGeschlechterrolle() {
		ArrayList<String> keywords = new ArrayList<String>();
		keywords.add("frauenarbeit");
		keywords.add("china");
		keywords.add("Arbeitsbedingungen");
		keywords.add("Geschlechterrolle");
		ResultList resultList = PartnerRecommenderTestHelper.getRecommendations("eexcess-partner-zbw-1.0-SNAPSHOT",	
				port, PartnerRecommenderTestHelper.createParamsForPartnerRecommender(20, keywords));
	    
        assertNotNull(resultList);
        assertTrue(resultList.results.size() > 0 );
        assertEquals(20, resultList.results.size());

	}
}
