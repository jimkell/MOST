package edu.rutgers.MOST.optimization.solvers;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JOptionPane;

import edu.rutgers.MOST.config.LocalConfig;
import edu.rutgers.MOST.data.Solution;
import edu.rutgers.MOST.optimization.GDBB.GDBB;
import edu.rutgers.MOST.presentation.GraphicalInterface;
import edu.rutgers.MOST.presentation.GraphicalInterfaceConstants;
import gurobi.GRB;
import gurobi.GRBCallback;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBModel;
import gurobi.GRBVar;
import gurobi.GRBLinExpr;

public class GurobiSolver extends Solver
{
	private enum SolverKind
	{
		FBASolver, GDBBSolver
	}

	private ArrayList< GRBVar > vars = new ArrayList< GRBVar >();
	private ArrayList< Double > soln = new ArrayList< Double >();
	private double objval;
	private GRBModel model;
	private GRBEnv env;
	private ObjType objType;
	private SolverKind solverKind = SolverKind.FBASolver; // default may change
															// in SetVar()

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

	public GurobiSolver()
	{
		try
		{
			// set up environment and the model/problem objects
			env = new GRBEnv();
			env.set( GRB.DoubleParam.IntFeasTol, 1.0E-9 );
			env.set( GRB.DoubleParam.FeasibilityTol, 1.0E-9 );
			env.set( GRB.IntParam.OutputFlag, 0 );
			model = new GRBModel( env );

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
							if( solverKind == SolverKind.GDBBSolver )
							{
								GDBB.intermediateSolution.add( new Solution(
										this.getDoubleInfo( GRB.CB_MIPSOL_OBJ ),
										this.getSolution( model.getVars() ) ) );
							}
							objval = getDoubleInfo( GRB.CB_MIPSOL_OBJ ); // MIP
																			// objective
						}
					}
					catch ( GRBException e )
					{
						e.printStackTrace();
					}
				}

			} );
		}
		catch ( GRBException e )
		{
			abort();
			String errMsg;
			switch( e.getErrorCode() )
			{
			case GRB.Error.NO_LICENSE:
				errMsg = "No validation file - run 'grbgetkey' to refresh it.";
				break;
			case GRB.Error.FAILED_TO_CREATE_MODEL:
				errMsg = "Gurobi failed to create the model";
			case GRB.Error.NOT_SUPPORTED:
				errMsg = "This optimization is not supported by Gurobi";
				break;
			case GRB.Error.INVALID_ARGUMENT:
				errMsg = "Gurobi encountered an invalid argument";
				break;
			case GRB.Error.IIS_NOT_INFEASIBLE:
				errMsg = "Gurobi determined the IIS is not feasable";
				break;
			case GRB.Error.NUMERIC:
				errMsg = "Gurobi encountered a numerical error while optimizing the model";
				break;
			case GRB.Error.INTERNAL:
				errMsg = "Gurobi has encountered an internal error!";
				break;
			default:
				errMsg = "Gurobi encountered an error optimizing the model";
			}
			LocalConfig.getInstance().hasValidGurobiKey = false;
			if( GraphicalInterface.getGdbbDialog() != null )
				GraphicalInterface.getGdbbDialog().setVisible( false );

			Object[] options = { "    OK    " };
			JOptionPane.showOptionDialog( null, "Error: " + errMsg,
					GraphicalInterfaceConstants.GUROBI_KEY_ERROR_TITLE,
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
					null, options, options[0] );
			LocalConfig.getInstance().getOptimizationFilesList().clear();
			// Do we really want to ever print stack trace? Need dialog to display this
			// printout as a message if one of the above conditions are not met.
			e.printStackTrace();
		}
		catch ( UnsatisfiedLinkError except )
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
					+ msg3 + "\n" + msg4,
					GraphicalInterfaceConstants.GUROBI_KEY_ERROR_TITLE,
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
					null, options, options[0] );
			// Do we really want to ever print stack trace? Need dialog to display this
			// printout as a message if one of the above conditions are not met.
			except.printStackTrace();
		}
		catch ( Exception except )
		{
			Object[] options = { "    OK    " };
			JOptionPane.showOptionDialog( null, except.getMessage(),
					GraphicalInterfaceConstants.GUROBI_KEY_ERROR_TITLE,
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
					null, options, options[0] );
			except.printStackTrace();
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
			if( varName == null || types == null || model == null )
				return;
			vars.add( model.addVar( lb, ub, 0.0, getGRBVarType( types ),
					varName ) );
			model.update();
		}
		catch ( GRBException e )
		{
			e.printStackTrace();
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
			GRBLinExpr expr = new GRBLinExpr();

			// add the terms
			for( Entry< Integer, Double > entry : map.entrySet())
				expr.addTerm( entry.getValue(), vars.get( entry.getKey() ) );

			// set the objective
			model.setObjective( expr, getGRBObjType( objType ) );
		}
		catch ( GRBException e )
		{
			e.printStackTrace();
		}

	}
	@Override
	public void addConstraint( Map< Integer, Double > map, ConType con,
			double value )
	{
		try
		{
			// row definitions
			GRBLinExpr expr = new GRBLinExpr();

			for( Entry< Integer, Double > entry : map.entrySet())
			{
				// add the terms in the expression
				int key = entry.getKey();
				double kvalue = entry.getValue();
				expr.addTerm( kvalue, vars.get( key ) );
			}
			// add the constraint
			model.addConstr( expr, getGRBConType( con ), value, null );
		}
		catch ( GRBException e )
		{
			e.printStackTrace();
		}
	}

	@Override
	public double optimize()
	{
		try
		{
			// preform the optimization and get the objective value
			model.optimize();
			objval = model.get( GRB.DoubleAttr.ObjVal );

			// get the flux values
			for( GRBVar var : vars)
				soln.add( var.get( GRB.DoubleAttr.X ) );

			// clean up
			model.dispose();
			env.dispose();
			vars.clear();
		}
		catch ( GRBException e )
		{
			e.printStackTrace();
		}

		return objval;
	}
	@Override
	public void setEnv( double timeLimit, int numThreads )
	{
		// TODO Auto-generated method stub
	}
	@Override
	public void setVars( VarType[] types, double[] lb, double[] ub )
	{
		// TODO Auto-generated method stub
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
}