package edu.rutgers.MOST.presentation;

import java.awt.Dimension;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class GurobiParameters {

	public static final String TITLE = "Gurobi Parameters";
	// names of parameters from http://www.gurobi.com/documentation/5.6/reference-manual/parameters
	
	public static final String GUROBI_PARAMETERS_MENU_ITEM = "Set Gurobi Properties";
	
	public static final String FEASIBILITYTOL_NAME = "Feasibility Tolerance";
	public static final double FEASIBILITYTOL_DEFAULT_VALUE = 1.0E-6;
	public static final double FEASIBILITYTOL_MINIMUM_VALUE = 1.0E-9;
	public static final double FEASIBILITYTOL_MAXIMUM_VALUE = 1.0E-2;
	
	public static final String INTFEASIBILITYTOL_NAME = "Integer Feasibility Tolerance";
	public static final double INTFEASIBILITYTOL_DEFAULT_VALUE = 1.0E-5;
	public static final double INTFEASIBILITYTOL_MINIMUM_VALUE = 1.0E-9;
	public static final double INTFEASIBILITYTOL_MAXIMUM_VALUE = 1.0E-1;
	
	public static final String OPTIMALITYTOL_NAME = "Dual Feasibility Tolerance";
	public static final double OPTIMALITYTOL_DEFAULT_VALUE = 1.0E-6;
	public static final double OPTIMALITYTOL_MINIMUM_VALUE = 1.0E-9;
	public static final double OPTIMALITYTOL_MAXIMUM_VALUE = 1.0E-2;
	
	public static final String HEURISTICS_NAME = "Heuristics";
	public static final double HEURISTICS_DEFAULT_VALUE = 0.05;
	public static final double HEURISTICS_MINIMUM_VALUE = 0;
	public static final double HEURISTICS_MAXIMUM_VALUE = 1;
	
	public static final String MIPFOCUS_NAME = "MIP Focus";
	public static final int MIPFOCUS_DEFAULT_VALUE = 0;
	public static final int MIPFOCUS_MINIMUM_VALUE = 0;
	public static final int MIPFOCUS_MAXIMUM_VALUE = 3;
	
	public static final String NUM_THREADS_NAME = "Number of Threads";
	public static final int MAX_NUM_THREADS = Runtime.getRuntime().availableProcessors();
	
	// layout
	public static final int DIALOG_WIDTH = 400;
	public static final int DIALOG_HEIGHT = 320;
	
	// layout constants
	public static final int COMPONENT_WIDTH = 150;
	public static final int COMPONENT_HEIGHT = 25;
	public static final int LABEL_WIDTH = 200;
	public static final int LABEL_HEIGHT = 25;
	
	public static final int LABEL_TOP_BORDER_SIZE = 10;
	public static final int LABEL_BOTTOM_BORDER_SIZE = 10;
	
	protected ArrayList< AbstractSavableObjectInterface > components = new ArrayList< AbstractSavableObjectInterface >();
	
	public ArrayList< AbstractSavableObjectInterface > getSavableParameters()
	{
		return components;
	}
	
	public JPanel getDialogPanel()
	{		
		JPanel result = new JPanel();
		Box vBox = Box.createVerticalBox();
		
		// segmented parameters
		ArrayList< JComponent > textParams = new ArrayList< JComponent >();

		textParams.add( new ADRealSegmentJTextFieldParameter( FEASIBILITYTOL_NAME, FEASIBILITYTOL_MINIMUM_VALUE,
				FEASIBILITYTOL_MAXIMUM_VALUE, FEASIBILITYTOL_DEFAULT_VALUE) );
		
		textParams.add( new ADRealSegmentJTextFieldParameter( INTFEASIBILITYTOL_NAME, INTFEASIBILITYTOL_MINIMUM_VALUE,
				INTFEASIBILITYTOL_MAXIMUM_VALUE, INTFEASIBILITYTOL_DEFAULT_VALUE) );
		
		textParams.add( new ADRealSegmentJTextFieldParameter( OPTIMALITYTOL_NAME, OPTIMALITYTOL_MINIMUM_VALUE,
				OPTIMALITYTOL_MAXIMUM_VALUE, OPTIMALITYTOL_DEFAULT_VALUE) );
		
		textParams.add( new ADRealSegmentJTextFieldParameter( HEURISTICS_NAME, HEURISTICS_MINIMUM_VALUE,
				HEURISTICS_MAXIMUM_VALUE, HEURISTICS_DEFAULT_VALUE ) );
		
		for( JComponent textparam : textParams )
		{
			textparam.setPreferredSize( new Dimension( COMPONENT_WIDTH, COMPONENT_HEIGHT ) );
			JLabel label = new JLabel();
			label.setText( textparam.getName() );
			label.setPreferredSize( new Dimension( LABEL_WIDTH, LABEL_HEIGHT ) );
			
			if( textparam instanceof ADRealSegmentJTextFieldParameter )
			{
				ADRealSegmentJTextFieldParameter param = (ADRealSegmentJTextFieldParameter)textparam;
				param.setToolTipText( "real values from " + param.minVal + " to " + param.maxVal );
			}
			else if ( textparam instanceof ADIntegerSegmentJTextFieldParameter )
			{
				ADIntegerSegmentJTextFieldParameter param = (ADIntegerSegmentJTextFieldParameter)textparam;
				param.setToolTipText( "real values from " + param.minVal + " to " + param.maxVal );
			
			}
			
			JPanel content = new JPanel();
			Box hBox = Box.createHorizontalBox();
			hBox.add( label );
			hBox.add( textparam );
			content.add( hBox );
			vBox.add( content );
			
			components.add( (AbstractSavableObjectInterface)textparam );
		}
		
		// combo boxes
		
		ArrayList< JComponent > cbParams = new ArrayList< JComponent >();
		
		cbParams.add( new ADIntegerSegmentJComboBoxParameter( MIPFOCUS_NAME, MIPFOCUS_MINIMUM_VALUE,
				MIPFOCUS_MAXIMUM_VALUE, MIPFOCUS_DEFAULT_VALUE) );
		
		cbParams.add( new ADIntegerSegmentJComboBoxParameter( NUM_THREADS_NAME,
				1, MAX_NUM_THREADS, MAX_NUM_THREADS ) );
		
		for( JComponent cbParam : cbParams )
		{
			cbParam.setPreferredSize( new Dimension( COMPONENT_WIDTH, COMPONENT_HEIGHT ) );
			JLabel label = new JLabel();
			label.setText( cbParam.getName() );
			label.setPreferredSize( new Dimension( LABEL_WIDTH, LABEL_HEIGHT ) );
			
			JPanel content = new JPanel();
			Box hBox = Box.createHorizontalBox();
			hBox.add( label );
			hBox.add( cbParam );
			content.add( hBox );
			vBox.add( content );
			
			components.add( (AbstractSavableObjectInterface)cbParam );
		}
		
		
		JPanel panel = new JPanel();
		panel.add( vBox );
		result.add( panel );
		
		return result;
	}
}