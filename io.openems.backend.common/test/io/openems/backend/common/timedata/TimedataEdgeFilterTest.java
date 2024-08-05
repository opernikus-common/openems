package io.openems.backend.common.timedata;

import org.junit.Assert;
import org.junit.Test;

public class TimedataEdgeFilterTest {

	private static final String DUMMY_COMP_ID = "comp0";
	private static final String TEST_EDGE_1 = "edge-1011555";
	private static final String TEST_EDGE_2 = "FOO";
	private static final String TEST_EDGE_3 = "edge-1011";
	private static final String[] TEST_FILTER = { "1011", "Foo", "Bar", "", "1012" };
	private static final String[] TEST_FILTER_2 = { "" };

	@Test
	public void testFilter() {
		var filter = new TimedataEdgeFilter(DUMMY_COMP_ID, TEST_FILTER);
		Assert.assertTrue(filter._isEdgeIdApplicable(TEST_EDGE_1));
		Assert.assertFalse(filter._isEdgeIdApplicable(TEST_EDGE_2));
		Assert.assertTrue(filter._isEdgeIdApplicable(TEST_EDGE_3));
		filter = new TimedataEdgeFilter(DUMMY_COMP_ID, TEST_FILTER_2);
		Assert.assertTrue(filter._isEdgeIdApplicable(TEST_EDGE_2));
		Assert.assertTrue(filter._isEdgeIdApplicable(TEST_EDGE_3));
	}

}
