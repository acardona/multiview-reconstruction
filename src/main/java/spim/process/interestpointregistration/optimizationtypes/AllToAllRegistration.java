package spim.process.interestpointregistration.optimizationtypes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.Illumination;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.ViewId;
import spim.fiji.spimdata.SpimData2;
import spim.process.interestpointregistration.ChannelProcess;
import spim.process.interestpointregistration.MatchPointList;
import spim.process.interestpointregistration.PairwiseMatch;

public class AllToAllRegistration extends GlobalOptimizationType
{
	public AllToAllRegistration(
			final SpimData2 spimData,
			final List< Angle > anglesToProcess,
			final List< ChannelProcess > channelsToProcess,
			final List< Illumination > illumsToProcess,
			final List< TimePoint > timepointsToProcess,
			final boolean save,
			final boolean considerTimePointsAsUnit )
	{ 
		super( spimData, anglesToProcess, channelsToProcess, illumsToProcess, timepointsToProcess, save, considerTimePointsAsUnit );
	}

	@Override
	public List< GlobalOptimizationSubset > assembleAllViewPairs()
	{
		final HashMap< ViewId, MatchPointList > allPointLists = new HashMap< ViewId, MatchPointList >();
		
		// collect all point lists from all timepoints
		for ( final TimePoint timepoint : timepointsToProcess )
		{
			final HashMap< ViewId, MatchPointList > pointLists = this.getInterestPoints( timepoint );
			
			allPointLists.putAll( pointLists );
		}
		
		// all viewids of all timepoints
		final ArrayList< ViewId > views = new ArrayList< ViewId >();
		views.addAll( allPointLists.keySet() );
		Collections.sort( views );

		// all pairs that need to be compared
		final ArrayList< PairwiseMatch > viewPairs = new ArrayList< PairwiseMatch >();		

		for ( int a = 0; a < views.size() - 1; ++a )
			for ( int b = a + 1; b < views.size(); ++b )
			{
				final ViewId viewIdA = views.get( a );
				final ViewId viewIdB = views.get( b );
				
				final MatchPointList listA = allPointLists.get( viewIdA );
				final MatchPointList listB = allPointLists.get( viewIdB );
				
				// in case we consider timepoints as units and the pair has the same timepoint, do not add;
				// i.e. add the pair always if the above statement is false
				if ( !( considerTimePointsAsUnit && ( viewIdA.getTimePointId() == viewIdB.getTimePointId() ) ) )
					viewPairs.add( new PairwiseMatch( viewIdA, viewIdB, listA, listB ) );
			}

		final ArrayList< GlobalOptimizationSubset > list = new ArrayList< GlobalOptimizationSubset >();
		list.add( new GlobalOptimizationSubset( viewPairs, "all-to-all matching over all timepoints" ) );
		
		return list;
	}
}
