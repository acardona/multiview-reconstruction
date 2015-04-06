package mpicbg.pointdescriptor.model;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

import java.util.Collection;

import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.PointMatch;

/**
 * 3d-rigid transformation models to be applied to points in 3d-space.
 * 
 * This function uses the method by Horn, using quaternions:
 * Closed-form solution of absolute orientation using unit quaternions,
 * Horn, B. K. P., Journal of the Optical Society of America A,
 * Vol. 4, page 629, April 1987
 * 
 * @author Johannes Schindelin (quaternion logic and implementation) and Stephan Preibisch
 * @version 0.1b
 * 
 */
public class TranslationInvariantSimilarityModel3D extends TranslationInvariantModel<TranslationInvariantSimilarityModel3D> 
{
	static final protected int MIN_NUM_MATCHES = 3;
	
	protected double
		m00 = 1.0, m01 = 0.0, m02 = 0.0,
		m10 = 0.0, m11 = 1.0, m12 = 0.0,
		m20 = 0.0, m21 = 0.0, m22 = 1.0;

	final protected double[][] N = new double[4][4];
	
	@Override
	public boolean canDoNumDimension( final int numDimensions ) { return numDimensions == 3; }

	@Override
	final public <P extends PointMatch> void fit( final Collection< P > matches )
		throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		final int numMatches = matches.size(); 
		if ( numMatches < MIN_NUM_MATCHES )
			throw new NotEnoughDataPointsException( matches.size() + " data points are not enough to estimate a 3d similarity model, at least " + MIN_NUM_MATCHES + " data points required." );

		double c1x, c1y, c1z, c2x, c2y, c2z;
		c1x = c1y = c1z = c2x = c2y = c2z = 0;

		for ( final PointMatch m : matches )
		{
			final double[] p = m.getP1().getL(); 
			final double[] q = m.getP2().getW();
			
			c1x += p[ 0 ];
			c1y += p[ 1 ];
			c1z += p[ 2 ];
			c2x += q[ 0 ];
			c2y += q[ 1 ];
			c2z += q[ 2 ];
		}
		c1x /= numMatches;
		c1y /= numMatches;
		c1z /= numMatches;
		c2x /= numMatches;
		c2y /= numMatches;
		c2z /= numMatches;

		double r1 = 0, r2 = 0;
		for ( final PointMatch m : matches )
		{
			final double[] p = m.getP1().getL(); 
			final double[] q = m.getP2().getW();
			
			double x1 = p[ 0 ] - c1x;
			double y1 = p[ 1 ] - c1y;
			double z1 = p[ 2 ] - c1z;
			double x2 = q[ 0 ] - c2x;
			double y2 = q[ 1 ] - c2y;
			double z2 = q[ 2 ] - c2z;
			r1 += x1 * x1 + y1 * y1 + z1 * z1;
			r2 += x2 * x2 + y2 * y2 + z2 * z2;
		}
		final double s = Math.sqrt(r2 / r1);
		
		// calculate N
		double Sxx, Sxy, Sxz, Syx, Syy, Syz, Szx, Szy, Szz;
		Sxx = Sxy = Sxz = Syx = Syy = Syz = Szx = Szy = Szz = 0;
		for ( final PointMatch m : matches )
		{
			final double[] p = m.getP1().getL(); 
			final double[] q = m.getP2().getW();
			
			final double x1 = (p[ 0 ] - c1x) * s;
			final double y1 = (p[ 1 ] - c1y) * s;
			final double z1 = (p[ 2 ] - c1z) * s;
			final double x2 = q[ 0 ] - c2x;
			final double y2 = q[ 1 ] - c2y;
			final double z2 = q[ 2 ] - c2z;
			Sxx += x1 * x2;
			Sxy += x1 * y2;
			Sxz += x1 * z2;
			Syx += y1 * x2;
			Syy += y1 * y2;
			Syz += y1 * z2;
			Szx += z1 * x2;
			Szy += z1 * y2;
			Szz += z1 * z2;
		}

		N[0][0] = Sxx + Syy + Szz;
		N[0][1] = Syz - Szy;
		N[0][2] = Szx - Sxz;
		N[0][3] = Sxy - Syx;
		N[1][0] = Syz - Szy;
		N[1][1] = Sxx - Syy - Szz;
		N[1][2] = Sxy + Syx;
		N[1][3] = Szx + Sxz;
		N[2][0] = Szx - Sxz;
		N[2][1] = Sxy + Syx;
		N[2][2] = -Sxx + Syy - Szz;
		N[2][3] = Syz + Szy;
		N[3][0] = Sxy - Syx;
		N[3][1] = Szx + Sxz;
		N[3][2] = Syz + Szy;
		N[3][3] = -Sxx - Syy + Szz;

		// calculate eigenvector with maximal eigenvalue
		/*
		final JacobiFloat jacobi = new JacobiFloat(N);
		final float[][] eigenvectors = jacobi.getEigenVectors();
		final float[] eigenvalues = jacobi.getEigenValues();
		int index = 0;
		for (int i = 1; i < 4; i++)
			if (eigenvalues[i] > eigenvalues[index])
				index = i;

		final float [] q = eigenvectors[index];
		final float q0 = q[0], qx = q[1], qy = q[2], qz = q[3];
		*/
		// calculate eigenvector with maximal eigenvalue

		final EigenvalueDecomposition evd = new EigenvalueDecomposition( new Matrix( N ) );
		
		final double[] eigenvalues = evd.getRealEigenvalues();
		final Matrix eigenVectors = evd.getV();

		int index = 0;
		for (int i = 1; i < 4; i++)
			if (eigenvalues[i] > eigenvalues[index])
				index = i;

		final double q0 = eigenVectors.get( 0, index ); 
		final double qx = eigenVectors.get( 1, index );
		final double qy = eigenVectors.get( 2, index );
		final double qz = eigenVectors.get( 3, index );

		// compute result

		// rotational part
		m00 = s * (q0 * q0 + qx * qx - qy * qy - qz * qz);
		m01 = s * 2 * (qx * qy - q0 * qz);
		m02 = s * 2 * (qx * qz + q0 * qy);
		m10 = s * 2 * (qy * qx + q0 * qz);
		m11 = s * (q0 * q0 - qx * qx + qy * qy - qz * qz);
		m12 = s * 2 * (qy * qz - q0 * qx);
		m20 = s * 2 * (qz * qx - q0 * qy);
		m21 = s * 2 * (qz * qy + q0 * qx);
		m22 = s * (q0 * q0 - qx * qx - qy * qy + qz * qz);
		
		/*
		// translational part
		result.apply(c1x, c1y, c1z);
		result.a03 = c2x - result.x;
		result.a13 = c2y - result.y;
		result.a23 = c2z - result.z;
		*/
	}
	
	@Override
	final public void set( final TranslationInvariantSimilarityModel3D m )
	{
		m00 = m.m00;
		m10 = m.m10;
		m20 = m.m20;
		m01 = m.m01;
		m11 = m.m11;
		m21 = m.m21;
		m02 = m.m02;
		m12 = m.m12;
		m22 = m.m22;		

		cost = m.cost;
	}

	@Override
	public TranslationInvariantSimilarityModel3D copy()
	{
		TranslationInvariantSimilarityModel3D m = new TranslationInvariantSimilarityModel3D();
		m.m00 = m00;
		m.m10 = m10;
		m.m20 = m20;
		m.m01 = m01;
		m.m11 = m11;
		m.m21 = m21;
		m.m02 = m02;
		m.m12 = m12;
		m.m22 = m22;
		
		m.cost = cost;

		return m;
	}
	
	@Override
	final public int getMinNumMatches(){ return MIN_NUM_MATCHES; }
	
	@Override
	final public double[] apply( final double[] l )
	{
		final double[] transformed = l.clone();
		applyInPlace( transformed );
		return transformed;
	}
	
	@Override
	final public void applyInPlace( final double[] l )
	{
		assert l.length == 3 : "3d affine transformations can be applied to 3d points only.";
		
		final double l0 = l[ 0 ];
		final double l1 = l[ 1 ];
		l[ 0 ] = l0 * m00 + l1 * m01 + l[ 2 ] * m02;
		l[ 1 ] = l0 * m10 + l1 * m11 + l[ 2 ] * m12;
		l[ 2 ] = l0 * m20 + l1 * m21 + l[ 2 ] * m22;
	}
	
	final public String toString()
	{
		return
			"3d-affine: (" +
			m00 + ", " + m01 + ", " + m02 + ", " +
			m10 + ", " + m11 + ", " + m12 + ", " +
			m20 + ", " + m21 + ", " + m22 + ")";
	}
	
}
