package spim.fiji.spimdata.interestpoints;

import mpicbg.models.Point;

/**
 * Single interest point, extends mpicbg Point by an id
 * 
 * @author Stephan Preibisch (stephan.preibisch@gmx.de)
 *
 */
public class InterestPoint extends Point
{
	private static final long serialVersionUID = 5615112297702152070L;

	protected final int id;

	public InterestPoint( final int id, final double[] l )
	{
		super( l );
		this.id = id;
	}
	
	public int getId() { return id; }
}
