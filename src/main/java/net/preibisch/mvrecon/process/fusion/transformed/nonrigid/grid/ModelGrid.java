package net.preibisch.mvrecon.process.fusion.transformed.nonrigid.grid;

import java.util.Collection;

import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import net.imglib2.Interval;
import net.imglib2.Localizable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.preibisch.mvrecon.fiji.plugin.fusion.FusionGUI;
import net.preibisch.mvrecon.process.fusion.FusionTools;
import net.preibisch.mvrecon.process.fusion.transformed.nonrigid.NonrigidIP;

public class ModelGrid implements RealRandomAccessible< NumericAffineModel3D >
{
	final int n;
	final long[] dim, min, controlPointDistance;
	final double alpha;

	// TODO: the min of the grid is handled independently of the actual randomaccessibleinterval, this is bad
	final RandomAccessibleInterval< NumericAffineModel3D > grid;

	public ModelGrid( final long[] controlPointDistance, final Interval boundingBox, final Collection< ? extends NonrigidIP > ips, final double alpha ) throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		this.n = boundingBox.numDimensions();
		this.alpha = alpha;

		if ( this.n != 3 )
			throw new RuntimeException( "Currently only 3d is supported by " + this.getClass().getName() );

		this.controlPointDistance = controlPointDistance;

		this.dim = new long[ n ];
		this.min = new long[ n ];

		for ( int d = 0; d < n; ++d )
		{
			this.min[ d ] = boundingBox.min( d );
			this.dim[ d ] = boundingBox.dimension( d ) / controlPointDistance[ d ] + 1;

			if ( boundingBox.dimension( d ) % controlPointDistance[ d ] != 0 )
				++this.dim[ d ];
		}

		this.grid = FusionTools.cacheRandomAccessibleInterval(
				new VirtualGrid(
						this.dim,
						this.min,
						this.controlPointDistance,
						this.alpha,
						ips ),
				FusionGUI.maxCacheSize,
				new NumericAffineModel3D(),
				new int[] { 3, 3, 3 } );
 
		/*
		final MovingLeastSquaresTransform2 transform = new MovingLeastSquaresTransform2();
		final ArrayList< PointMatch > matches = new ArrayList<>();

		for ( final NonrigidIP ip : ips )
			matches.add( new PointMatch( new Point( ip.getTargetW().clone() ), new Point( ip.getL().clone() ) ) );

		final AffineModel3D model = new AffineModel3D();

		transform.setAlpha( alpha );
		transform.setModel( model );
		transform.setMatches( matches );


		// iterate over all control points
		this.grid = new ListImg< NumericAffineModel3D >( dim, new NumericAffineModel3D( new AffineModel3D() ) );

		final ListLocalizingCursor< NumericAffineModel3D > it = ( (ListImg< NumericAffineModel3D >)grid ).localizingCursor();
		final double[] pos = new double[ n ];

		long time = System.currentTimeMillis();

		while ( it.hasNext() )
		{
			it.fwd();

			//System.out.print( Util.printCoordinates( it ) + " >>> " );

			getWorldCoordinates( pos, it, min, controlPointDistance, n );

			//System.out.print( Util.printCoordinates( pos ) );

			transform.applyInPlace( pos ); // also modifies the model

			it.set( new NumericAffineModel3D( model.copy() ) );

			//System.out.println( " >>> " + Util.printCoordinates( pos ) + ": " + model );
		}

		IOFunctions.println( new Date( System.currentTimeMillis() ) + ": computed grid [" + (System.currentTimeMillis() - time ) + " ms]." );
		*/
	}

	public ModelGrid( final long[] controlPointDistance, final Interval boundingBox, final Collection< ? extends NonrigidIP > ips ) throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		this( controlPointDistance, boundingBox, ips, 1.0 );
	}

	public double getAlpha() { return alpha; }
	/*
	protected static final void getWorldCoordinates( final double[] pos, final Localizable l, final long[] min, final long[] controlPointDistance, final int n )
	{
		for ( int d = 0; d < n; ++d )
			pos[ d ] = l.getLongPosition( d ) * controlPointDistance[ d ] + min[ d ];
	}
	*/
	@Override
	public int numDimensions() { return n; }

	@Override
	public RealRandomAccess< NumericAffineModel3D > realRandomAccess()
	{
		return new ModelGridAccess( this.grid, min, controlPointDistance );
	}

	@Override
	public RealRandomAccess< NumericAffineModel3D > realRandomAccess( final RealInterval interval ) { return realRandomAccess(); }
}
