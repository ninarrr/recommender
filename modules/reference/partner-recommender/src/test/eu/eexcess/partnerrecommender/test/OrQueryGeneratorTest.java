package eu.eexcess.partnerrecommender.test;

import static org.junit.Assert.*;

import org.junit.Test;

import eu.eexcess.dataformats.userprofile.ContextKeyword;
import eu.eexcess.dataformats.userprofile.ExpansionType;
import eu.eexcess.dataformats.userprofile.SecureUserProfile;
import eu.eexcess.partnerrecommender.reference.OrQueryGenerator;

public class OrQueryGeneratorTest {

	


		private static final OrQueryGenerator gen = new OrQueryGenerator();

		@Test
		public void multibleExpansionsTest() {
			
			SecureUserProfile userProfile = new SecureUserProfile();
			ContextKeyword keyword1 = new ContextKeyword("k1");
			userProfile.contextKeywords.add(keyword1);
			ContextKeyword keyword2 = new ContextKeyword("k2");
			userProfile.contextKeywords.add(keyword2);
			ContextKeyword keyword3 = new ContextKeyword("k3");
			userProfile.contextKeywords.add(keyword3);
			ContextKeyword keyword4 = new ContextKeyword("k4");
			userProfile.contextKeywords.add(keyword4);
			ContextKeyword keyword5 = new ContextKeyword("k5");
			keyword5.expansion=ExpansionType.EXPANSION;
			userProfile.contextKeywords.add(keyword5);
			ContextKeyword keyword6 = new ContextKeyword("k6");
			keyword6.expansion=ExpansionType.EXPANSION;
			userProfile.contextKeywords.add(keyword6);
			ContextKeyword keyword7 = new ContextKeyword("k7");
			keyword7.expansion=ExpansionType.EXPANSION;
			userProfile.contextKeywords.add(keyword7);
			String result =gen.toQuery(userProfile );
			System.out.println(result);
			assertTrue(result.equals("\"k1\" \"k2\" \"k3\" \"k4\" OR \"k5\" OR \"k6\" OR \"k7\""));
		}

}



