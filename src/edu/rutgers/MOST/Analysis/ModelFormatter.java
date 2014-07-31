package edu.rutgers.MOST.Analysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import au.com.bytecode.opencsv.CSVReader;
import edu.rutgers.MOST.config.LocalConfig;
import edu.rutgers.MOST.data.Model;
import edu.rutgers.MOST.data.ModelParser;
import edu.rutgers.MOST.data.SBMLReaction;

public class ModelFormatter
{
	public Vector< Double > formatFluxBoundsfromGeneExpressionData( File file, Model model ) throws FileNotFoundException
	{		
		Vector< Double > gene_expr = new Vector< Double >();
		if( file == null || !file.exists() )
		{
			throw new FileNotFoundException( "formatFluxBoundsfromGeneExpressionData" );
		}
		try
		{
			
			CSVReader csvReader = new CSVReader( new FileReader( file ) );
			List< String[] > all = csvReader.readAll();
			csvReader.close();
			Map< String, Double > expressionLevels = new HashMap< String, Double >();
			Map< String, Vector< Double > > levels = new HashMap< String, Vector< Double > >();
			
			for( String[] keyval : all )
			{
				if( !levels.containsKey( keyval[0] ) )
					levels.put( keyval[0], new Vector< Double >() );

				levels.get( keyval[0] ).add( Double.valueOf( keyval[1] ) );
			}
			for( String[] keyval : all )
			{
				Double val = 0.0;
				for( Double d : levels.get( keyval[0] ))
					val += d;
				val = val / levels.get( keyval[0] ).size();
				expressionLevels.put( keyval[0], val );
			}

			for( SBMLReaction reaction : model.getReactions() )
			{
				ModelParser parser = new ModelParser( reaction.getGeneAssociation(), expressionLevels )
				{
					@Override
					protected Double substitute( String token )
					{
						System.out.println( "Gene \"" + token + "\" not in database, replacing val with infinity" );
						return Double.POSITIVE_INFINITY;
					}
				};
				double fluxBound = parser.getValue();
				reaction.setLowerBound( reaction.getLowerBound() >= 0.0 ? 0.0 : -fluxBound );
				reaction.setUpperBound( reaction.getUpperBound() <= 0.0 ? 0.0 : fluxBound  );
				gene_expr.add( fluxBound );
			}
			model.setReactions( model.getReactions() );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
		return gene_expr;
	}
	Vector< Double > parseGeneExpressionDataSPOT( File file, Model model, boolean originalSPOT ) throws Exception
	{
		Vector< Double > gene_expr = new Vector< Double >();
		if( file == null || !file.exists() )
			throw new FileNotFoundException( "parseGeneExpressionDataSPOT" );
		try
		{
			
			CSVReader csvReader = new CSVReader( new FileReader( file ) );
			List< String[] > all = csvReader.readAll();
			csvReader.close();
			Map< String, Double > expressionLevels = new HashMap< String, Double >();
			Map< String, Vector< Double > > levels = new HashMap< String, Vector< Double > >();
			
			for( String[] keyval : all )
			{
				if( !levels.containsKey( keyval[0] ) )
					levels.put( keyval[0], new Vector< Double >() );

				levels.get( keyval[0] ).add( Double.valueOf( keyval[1] ) );
			}
			for( String[] keyval : all )
			{
				Double val = 0.0;
				for( Double d : levels.get( keyval[0] ))
					val += d;
				val = val / levels.get( keyval[0] ).size();
				expressionLevels.put( keyval[0], val );
			}

			for( SBMLReaction reaction : model.getReactions() )
			{
				ModelParser parser = new ModelParser( reaction.getGeneAssociation(), expressionLevels )
				{
					@Override
					protected Double substitute( String token )
					{
						System.out.println( "Gene \"" + token + "\" not in database, replacing val with infinity" );
						return Double.POSITIVE_INFINITY;
					}
				};
				Double parse_expr = new Double( parser.getValue() );
				
				if( originalSPOT )
				{
					if( !LocalConfig.getInstance().getConstantBoundsIdList().contains( reaction.getId() ) )
					{
						reaction.setLowerBound( reaction.getLowerBound() < 0.0 ? Double.NEGATIVE_INFINITY : 0.0 );
						reaction.setUpperBound( reaction.getUpperBound() > 0.0 ? Double.POSITIVE_INFINITY : 0.0 );
					}
					
					if( parse_expr.isInfinite() || reaction.getReversible().toLowerCase().equals( "true" ) )
						gene_expr.add( 0.0 );
					else
						gene_expr.add( parse_expr );
					}
				else
				{
					if( !parse_expr.isInfinite() && reaction.getLowerBound() >= 0.0 )
						gene_expr.add( parse_expr );
					else if( !parse_expr.isInfinite() && reaction.getUpperBound() <= 0.0 )
						gene_expr.add( -parse_expr );
					else
						gene_expr.add( 0.0 );
				}
			}
			
			model.setReactions( model.getReactions() );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
		return gene_expr;
	}
}
