package edu.rutgers.MOST.optimization.solvers;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import org.coinor.Ipopt;

import edu.rutgers.MOST.presentation.GraphicalInterfaceConstants;
import edu.rutgers.MOST.presentation.ResizableDialog;
import edu.rutgers.MOST.presentation.GraphicalInterface;
import edu.rutgers.MOST.Analysis.GDBB;
import edu.rutgers.MOST.config.LocalConfig;
import edu.rutgers.MOST.data.Solution;
import gurobi.GRB;
import gurobi.GRBCallback;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBModel;
import gurobi.GRBQuadExpr;
import gurobi.GRBVar;
import gurobi.GRBLinExpr;

public class GurobiSolver extends Solver
{	
	private class RowEntry
	{
		public int idx;
		public double coef;

		RowEntry( int i, double v )
		{
			idx = i;
			coef = v;
		}
	}

	private class RowType
	{
		public double val;
		public char type;
		public Vector< RowEntry > entries = new Vector< RowEntry >();

		RowType( double v, char t )
		{
			val = v;
			type = t;
		}
	}

	private class ColumnType
	{
		public String name;
		public int type;
		public double lb;
		public double ub;

		ColumnType( String n, int t, double l, double u )
		{
			name = n;
			type = t;
			lb = l;
			ub = u;
		}
	}

	private class ObjectiveType
	{
		Vector< RowEntry > entries = new Vector< RowEntry >();
	}

	private Vector< RowType > rows = new Vector< RowType >();
	private Vector< ColumnType > columns = new Vector< ColumnType >();
	private ObjectiveType objective = new ObjectiveType();
	private ArrayList< Double > soln = new ArrayList< Double >();
	
	private double objval;
	private GRBEnv env = null;
	private ObjType objType;
	private ResizableDialog dialog = new ResizableDialog( "Error",
			"Gurobi Solver Error", "Gurobi Solver Error" );

	public static boolean isGurobiLinked()
	{
		try
		{
			try
			{
				GRBEnv env = new GRBEnv();
				env.dispose();
			}
			catch ( GRBException e ) // necessary due to throws declaration
			{
			}
		}
		catch ( UnsatisfiedLinkError | NoClassDefFoundError except )
		{
			return false; // gurobi does not link
		}

		return true; // gurobi does link
	}
	private void processStackTrace( Exception e )
	{
		//e.printStackTrace();
		StringWriter errors = new StringWriter();
		e.printStackTrace( new PrintWriter( errors ) );
		dialog.setErrorMessage( errors.toString() + "</p></html>" );
		// centers dialog
		dialog.setLocationRelativeTo(null);
		dialog.setModal(true);		
		dialog.setVisible( true );
	}
	private void promptGRBError( GRBException e )
	{
		abort();
		String errMsg;
		int code = e.getErrorCode();
		switch( code )
		{
		case GRB.Error.NO_LICENSE:
			errMsg = "<html><p>No validation file - run 'grbgetkey' to refresh it.</p></html>";
			LocalConfig.getInstance().hasValidGurobiKey = false;
			break;
		case GRB.Error.FAILED_TO_CREATE_MODEL:
			errMsg = "<html><p>Gurobi failed to create the model";
		case GRB.Error.NOT_SUPPORTED:
			errMsg = "<html><p>This optimization is not supported by Gurobi";
			break;
		case GRB.Error.INVALID_ARGUMENT:
			errMsg = "<html><p>Gurobi encountered an invalid argument";
			break;
		case GRB.Error.IIS_NOT_INFEASIBLE:
			errMsg = "<html><p>Gurobi determined the IIS is not feasable";
			break;
		case GRB.Error.NUMERIC:
			errMsg = "<html><p>Gurobi encountered a numerical error while optimizing the model";
			break;
		case GRB.Error.INTERNAL:
			errMsg = "<html><p>Gurobi has encountered an internal error!";
			break;
		case 0:
			errMsg = e.getMessage() + "\n";
			break;
		default:
			errMsg = "<html><p>Gurobi encountered an error optimizing the model - <br> "
					+ " <a href=" + GraphicalInterfaceConstants.GUROBI_ERROR_CODE_URL
					+ ">Error Code:" + code + "</a><br>\n";
		}
		if( GraphicalInterface.getGdbbDialog() != null )
			GraphicalInterface.getGdbbDialog().setVisible( false );

		processStackTrace( new Exception( errMsg ) );
		LocalConfig.getInstance().getOptimizationFilesList().clear();
	}
	private char getGRBVarType( VarType type )
	{
		switch( type )
		{
		case CONTINUOUS:
			return GRB.CONTINUOUS;
		case BINARY:
			return GRB.BINARY;
		case INTEGER:
			return GRB.INTEGER;
		case SEMICONT:
			return GRB.SEMICONT;
		case SEMIINT:
			return GRB.SEMIINT;
		default:
			return GRB.CONTINUOUS;
		}
	}
	private char getGRBConType( ConType type )
	{
		switch( type )
		{
		case LESS_EQUAL:
			return GRB.LESS_EQUAL;
		case EQUAL:
			return GRB.EQUAL;
		case GREATER_EQUAL:
			return GRB.GREATER_EQUAL;
		default:
			return GRB.LESS_EQUAL;
		}
	}
	private int getGRBObjType( ObjType objType )
	{
		switch( objType )
		{
		case Minimize:
			return GRB.MINIMIZE;
		case Maximize:
			return GRB.MAXIMIZE;
		default:
			return GRB.MINIMIZE;
		}
	}

	public GurobiSolver( Algorithm algorithm )
	{
		super( algorithm );
		// set the dialog
		final ArrayList< Image > icons = new ArrayList< Image >();
		icons.add( new ImageIcon( "etc/most16.jpg" ).getImage() );
		icons.add( new ImageIcon( "etc/most32.jpg" ).getImage() );
		dialog.setIconImages( icons );
		dialog.setLocationRelativeTo( null );
		dialog.setVisible( false );
		dialog.setDefaultCloseOperation( javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE );
		dialog.addWindowListener( new WindowAdapter()
		{
			public void windowClosing( WindowEvent evt )
			{
				dialog.setVisible( false );
			}
		} );

		dialog.OKButton.addActionListener( new ActionListener()
		{
			public void actionPerformed( ActionEvent ae )
			{
				dialog.setVisible( false );
			}

		} );

		if( !isGurobiLinked() )
		{
			String msg1 = "Java could not link to the Gurobi dependencies";
			String msg2 = "Please check if your Gurobi environment variables match the location of Gurobi dependencies";
			String msg3 = "Your " + System.getProperty( "sun.arch.data.model" )
					+ " bit JVM is trying to launch "
					+ System.getProperty( "sun.arch.data.model" )
					+ " bit Gurobi";
			String msg4 = "The current JVM specs are: "
					+ System.getProperty( "java.runtime.version" );
			Object[] options = { "    OK    " };
			JOptionPane.showOptionDialog( null, msg1 + "\n" + msg2 + "\n"
					+ msg3 + "\n" + msg4, "Linking Error",
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
					null, options, options[0] );
			return;
		}
		try
		{
			// set up environment and the model/problem objects
			if( env  == null )
				env = new GRBEnv();
			env.set( GRB.DoubleParam.IntFeasTol, 1.0E-9 );
			env.set( GRB.DoubleParam.FeasibilityTol, 1.0E-9 );
			env.set( GRB.IntParam.OutputFlag, 0 );
			
		}
		catch ( GRBException e )
		{
			promptGRBError( e );
		}
		catch ( Exception except ) // unexpected
		{
			processStackTrace( except );
		}

	}
	@Override
	public String getName()
	{
		return "GurobiSolver";
	}
	@Override
	public ArrayList< Double > getSoln()
	{
		// return the column list
		return soln;
	}
	@Override
	public void setVar( String varName, VarType types, double lb, double ub )
	{
		try
		{
			// column definitions
			if( varName == null || types == null  )
				return;
			
			columns.add( new ColumnType( varName, getGRBVarType( types ), lb, ub ) );
		}
		catch ( Exception e )
		{
			processStackTrace( e );
		}

	}
	@Override
	public void setObjType( ObjType objType )
	{
		this.objType = objType;
	}
	@Override
	public void setObj( Map< Integer, Double > map )
	{
		try
		{
			// objective definition
			for( Entry< Integer, Double > entry : map.entrySet())
				objective.entries.add( new RowEntry( entry.getKey(), entry
						.getValue() ) );
		}
		catch ( Exception e )
		{
			processStackTrace( e );
		}

	}
	@Override
	public void addConstraint( Map< Integer, Double > map, ConType con,
			double value )
	{
		try
		{
			// row / constraint definitions
			RowType row = new RowType( value, getGRBConType( con ) );
			for( Entry< Integer, Double > entry : map.entrySet() )
			{
				int key = entry.getKey();
				double kvalue = entry.getValue();
				row.entries.add( new RowEntry( key, kvalue ) );
			}
			rows.add( row );
		}
		catch ( Exception e )
		{
			processStackTrace( e );
		}
	}

	private double minimizeEuclideanNorm()
	{
		double result = 0.0;
		
		try
		{
			// set up the quadratic environment model
			GRBEnv quad_env = new GRBEnv();
			GRBModel quad_model = new GRBModel( env );
			ArrayList< GRBVar > vars = new ArrayList< GRBVar >();
			quad_env.set( GRB.DoubleParam.IntFeasTol, 1.0E-9 );
			quad_env.set( GRB.DoubleParam.FeasibilityTol, 1.0E-9 );
			quad_env.set( GRB.IntParam.OutputFlag, 0 );
			
			// create the variables
			for( ColumnType it : columns)
			{
				vars.add( quad_model.addVar( it.lb, it.ub, 0.0, (char)it.type,
						it.name ) );
			}
			quad_model.update();
			
			// add rows / constraints
			for( RowType it : rows)
			{
				GRBLinExpr expr = new GRBLinExpr();
				for( RowEntry entry : it.entries )
				{
					expr.addTerm( entry.coef, vars.get( entry.idx ) );
				}
				quad_model.addConstr( expr, it.type, it.val, null );
			}
			
			// add the Maximum objective constraint
			GRBLinExpr maxObj = new GRBLinExpr();
			for( RowEntry entry : objective.entries )
				maxObj.addTerm( entry.coef, vars.get( entry.idx ) );
			GRBVar objValue = quad_model.addVar( this.objval, this.objval, 0.0, GRB.CONTINUOUS, null );
			quad_model.update(); // due to adding a new variable
			quad_model.addConstr( maxObj, GRB.EQUAL, objValue, null );
			
			// set the objective
			GRBQuadExpr expr = new GRBQuadExpr();
			for( GRBVar var : quad_model.getVars() ) 
				expr.addTerm( 1.0, var, var );
			quad_model.setObjective( expr );
			
			// optimize the model
			quad_model.optimize();
			
			// get min of sum of min v^2
			// objval = result = quad_model.get( GRB.DoubleAttr.ObjVal );
			
			soln.clear();
			
			for( GRBVar var : vars)
				soln.add( var.get( GRB.DoubleAttr.X ) );
			
			// clean up
			quad_model.dispose();
			quad_env.dispose();
		}
		catch( GRBException e )
		{
			promptGRBError( e );
		}
		
		return result;
	}
	@Override
	public double optimize()
	{
		try
		{
			final GRBModel model = new GRBModel( env );
			ArrayList< GRBVar > vars = new ArrayList< GRBVar >();
			
			try
			{
				// set the callback
				model.setCallback( new GRBCallback()
				{
					@Override
					protected void callback()
					{
						try
						{
							if( abort )
								this.abort();
							else if( this.where == GRB.CB_SIMPLEX )
								objval = getDoubleInfo( GRB.CB_SPX_OBJVAL ); // FBA
																				// objective
							else if( this.where == GRB.CB_MIPSOL )
							{
								// GDBB intermediate solutions
								GDBB.intermediateSolution.add( new Solution( this
										.getDoubleInfo( GRB.CB_MIPSOL_OBJ ), this
										.getSolution( model.getVars() ) ) );
								objval = getDoubleInfo( GRB.CB_MIPSOL_OBJ ); // MIP
																				// objective
							}
						}
						catch ( GRBException e )
						{
							processStackTrace( e );
						}
					}
				} );
				
				// add columns
				for( ColumnType it : columns)
				{
					vars.add( model.addVar( it.lb, it.ub, 0.0, (char)it.type,
							it.name ) );
				}
				model.update();
				
	
				// add rows / constraints
				for( RowType it : rows)
				{
					GRBLinExpr expr = new GRBLinExpr();
					for( RowEntry entry : it.entries )
					{
						expr.addTerm( entry.coef, vars.get( entry.idx ) );
					}
					model.addConstr( expr, it.type, it.val, null );
				}
				
				
				// set the objective
				GRBLinExpr expr = new GRBLinExpr();
	
				// set the terms & coefficients defining the objective function
				for( RowEntry entry : objective.entries )
					expr.addTerm( entry.coef, vars.get( entry.idx ) );
	
				// set the objective
				model.setObjective( expr, getGRBObjType( objType ) );
				
				// perform the optimization and get the objective value
				model.optimize();
				if( !abort )
				{
					switch( model.get( GRB.IntAttr.Status ) )
					{
					case GRB.LOADED:
						throw new GRBException( "Model is loaded, but no solution information is available." );
					case GRB.INFEASIBLE:
						throw new GRBException( "Model was proven to be infeasible." );
					case GRB.INF_OR_UNBD:
						throw new GRBException( "Model was proven to be either infeasible or unbounded." );
					case GRB.UNBOUNDED:
						break;
					case GRB.CUTOFF:
						break;
					case GRB.NODE_LIMIT:
						break;
					case GRB.TIME_LIMIT:
						break;
					case GRB.SOLUTION_LIMIT:
						break;
					case GRB.INTERRUPTED:
						break;
					case GRB.NUMERIC:
						throw new GRBException( "Optimization was terminated due to unrecoverable numerical difficulties." );
					case GRB.SUBOPTIMAL:
						break;
					case GRB.INPROGRESS:
						break;
						
						
					}
					objval = model.get( GRB.DoubleAttr.ObjVal );
		
					// get the flux values
					for( GRBVar var : vars)
						soln.add( var.get( GRB.DoubleAttr.X ) );
					if( getAlgorithm() == Algorithm.Eflux2 )
						this.minimizeEuclideanNorm();
					
					if( getAlgorithm() == Algorithm.SPOT )
					{
						// set up the nlp
						double[] x_L = new double[ this.columns.size() ];
						double[] x_U = new double[ this.columns.size() ];
						
						// set the var upper and lower bounds
						for( int i = 0; i < this.columns.size(); ++i )
						{
							x_L[ i ] = this.columns.get( i ).lb;
							x_L[ i ] = this.columns.get( i ).ub;
						}
						
						double[] g_L = new double[ this.rows.size() ];
						double[] g_U = new double[ this.rows.size() ];
						
						// set the constraint upper and lower bounds
						for( int i = 0; i < this.rows.size(); ++i )
						{
							if( this.rows.get( i ).type == GRB.LESS_EQUAL )
							{
								g_L[ i ] = Double.NEGATIVE_INFINITY;
								g_U[ i ] = this.rows.get( i ).val;
							}
							else if( this.rows.get( i ).type == GRB.EQUAL )
							{
								g_L[ i ] = this.rows.get( i ).val;
								g_U[ i ] = this.rows.get( i ).val;
							}
							else if( this.rows.get( i ).type == GRB.GREATER_EQUAL )
							{
								g_L[ i ] = this.rows.get( i ).val;
								g_U[ i ] = Double.POSITIVE_INFINITY;
							}
						}
						
						this.create( this.columns.size(), x_L, x_U, this.rows.size(),
								g_L, g_U, this.getJacobianNonZeros( this.columns.size(), this.rows.size() ),
								this.getHeissanNonZeros( this.columns.size() ), Ipopt.C_STYLE );
					}
				}
			}
			catch( GRBException e )
			{
				throw e;
			}
			finally
			{
				// clean up
				columns.clear();
				rows.clear();
				model.dispose();
				env.dispose();
				vars.clear();
			}
		}
		catch ( GRBException e )
		{
			promptGRBError( e );
			return Double.NaN;
		}

		return objval;
	}
	@Override
	public void setEnv( double timeLimit, int numThreads )
	{
		if( env != null )
			return;
		
		try
		{
			env = new GRBEnv();
			env.set( GRB.DoubleParam.Heuristics, 1.0 );
			env.set( GRB.DoubleParam.ImproveStartGap, Double.POSITIVE_INFINITY );
			env.set( GRB.DoubleParam.TimeLimit, timeLimit );
			env.set( GRB.IntParam.MIPFocus, 1 );
			env.set( GRB.IntParam.Threads, numThreads );
		}
		catch ( GRBException e )
		{
			promptGRBError( e );
		}
	}
	@Override
	public void setVars( VarType[] types, double[] lb, double[] ub )
	{
	}
	@Override
	public void abort()
	{
		abort = true;
	}
	@Override
	public void enable()
	{
		abort = false;
	}
	@Override
	public void setAbort( boolean abort )
	{
		this.abort = abort;
	}
	@Override
	protected boolean eval_f( int n, double[] x, boolean new_x,
			double[] obj_value )
	{
		try
		{
			ArrayList< Double > flux_v = new ArrayList< Double >();
			ArrayList< Double > gene_v = new ArrayList< Double >();
			// fill in flux_v using variable 'x', fill in gene_v given value from file

			for( RowType row : rows )
			{
				Double g_i = row.val; // updated from SPOT.run() and modelFormatter method
				Double v_i = 0.0;
				for( RowEntry entry : row.entries )
				{
					v_i += entry.coef * x[ entry.idx ];
				}
				flux_v.add( v_i );
				gene_v.add( g_i );
			}

			// calculate the dot product between flux_v and gene_v
			double dotProduct = 0.0;
			assert( flux_v.size() == gene_v.size() );
			for( int i = 0; i < flux_v.size(); ++i )
				dotProduct += flux_v.get( i ) * gene_v.get( i );
			
			// calculate length of flux_v
			double length_flux_v = 0;
			for( Double d : flux_v )
				length_flux_v += d;
			length_flux_v = Math.sqrt( length_flux_v );
			
			// calculate length of gene_v
			double length_gene_v = 0;
			for( Double d : gene_v )
				length_gene_v += d;
			length_gene_v = Math.sqrt( length_gene_v );
			
			// -1 <= ( flux_v dot gene_v ) / ( ||flux_v|| ||gene_v|| ) <= 1
			obj_value[ 0 ] = dotProduct / ( length_flux_v * length_gene_v );
			
		}
		catch( Exception e )
		{
			e.printStackTrace();
		}
		return true;
	}
	@Override
	protected boolean eval_grad_f( int n, double[] x, boolean new_x,
			double[] grad_f )
	{
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	protected boolean eval_g( int n, double[] x, boolean new_x, int m,
			double[] g )
	{
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	protected boolean eval_jac_g( int n, double[] x, boolean new_x, int m,
			int nele_jac, int[] iRow, int[] jCol, double[] values )
	{
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	protected boolean eval_h( int n, double[] x, boolean new_x,
			double obj_factor, int m, double[] lambda, boolean new_lambda,
			int nele_hess, int[] iRow, int[] jCol, double[] values )
	{
		// TODO Auto-generated method stub
		return false;
	}
}