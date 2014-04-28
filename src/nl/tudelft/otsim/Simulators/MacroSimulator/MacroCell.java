package nl.tudelft.otsim.Simulators.MacroSimulator;

import java.awt.Color;
//import java.awt.geom.Point2D;
//import java.awt.geom.Point2D.Double;




import nl.tudelft.otsim.Simulators.MacroSimulator.Model;

import nl.tudelft.otsim.GUI.GraphicsPanel;


import java.util.ArrayList;

import java.util.Random;


import nl.tudelft.otsim.GeoObjects.Vertex;


/**
 * A single cell of road. Different cells are connected in the
 * longitudinal or lateral direction. The <tt>jMacroCell</tt> object also provides a
 * few network utilities to get traffic state information.
 * <br>
 * <br>
 */
public class MacroCell {
	// TODO: delete unnecessary variables and methods! 
	// TODO: merges and splits at nodes
	// TODO: boundary conditions
	
	
	private double width = 0;
	public ArrayList<Vertex> vertices = new ArrayList<Vertex>();
	//private int id;
	//private double speedLimit;
	public ArrayList<Integer> ins = new ArrayList<Integer>();
	public ArrayList<Integer> outs = new ArrayList<Integer>();
	
	// Geographical info
    /** Array of x-coordinates defining the lane curvature. */
    public double[] x;

    /** Array of y-coordinates defining the lane curvature. */
    public double[] y;

    /** Length of the lane [m]. */
    public double l;

    /** Main model. */
    final public Model model;

	/** ID of cell for user recognition. */
    public int id;

    /** Set of upstream cells in case of a merge. */
    public java.util.ArrayList<MacroCell> ups = new java.util.ArrayList<MacroCell>();

    /** Set of downstream cells in case of a split. */
    public java.util.ArrayList<MacroCell> downs = new java.util.ArrayList<MacroCell>();
    
    /** Set of upstream cells in case of a merge. */
    public java.util.ArrayList<Integer> upsInt = new java.util.ArrayList<Integer>();

    /** Set of downstream cells in case of a split. */
    public java.util.ArrayList<Integer> downsInt = new java.util.ArrayList<Integer>();

    /** Left cell (if any). for multi-lane modeling */
    public MacroCell left;

    /** Right cell (if any). */
    public MacroCell right;
    
    public Node nodeIn;
    
    public Node nodeOut;
    
    public int indexNodeIn;
    public int indexNodeOut;

    /** Destination number, NODESTINATION if no destination. */
    public int destination;

    /** Origin number, NOORIGIN if no origin. */
    public int origin;

    //Traffic states
    /** Flow in this cell. [veh/h or veh/s] */
    public double QCell;
    
    /** Density in this cell. [veh/km or veh/m] */
    public double KCell = 5;
    
    /** Average spacing in this cell. [km or m] */
    public double SCell;
    
    /** Average speed in this cell. [km/h or m/s] */
    public double VCell;
    
    /** Flux into this cell. [veh/h or veh/s] */
    public double FluxIn;
    
    /** Flux out from this cell. [veh/h or veh/s] */
    public double FluxOut;
    
    /** Supply from this cell. [veh/h or veh/s] */
    public double Supply;
    
    /** Demand out from this cell. [veh/h or veh/s] */
    public double Demand;
    public FD fd;
    
    // Parameters    
    /** Legal speed limit [km/h]. */
    public double vLim = 120;
    
    /** Legal critical density [veh/km]. */
    public double kCri = 18;
    
    /** Legal jam density [veh/km]. */
    public double kJam = 125;
    
    /** Legal flow capacity [veh/h/lane]. */
    public double qCap;
    
    public double[] FluxIn2;
	public double[] FluxOut2;
	public int lanes;
	
	
    
    /**
     * Constructor that will calculate the lane length from the x and y
     * coordinates.
     * @param x X coordinates of curvature.
     * @param y Y coordinates of curvature.
     * @param id User recognizable lane id.
     * @param model Main model.
     */
    public MacroCell(Model model, double[] x, double[] y, int id) {
        this.model = model;
        this.x = x;
        this.y = y;
        this.id = id;
        l = calcLength();
        
    }
    public MacroCell(Model model, double length, int id) {
        this.model = model;
        this.l = length;
        //this.x = {0,0};
        //this.y = {0,length};
        this.id = id;
             
    }
    public MacroCell(Model model) {
    	this.model = model;
    	this.l = calcLength();
    }
    public void init() {
    	lanes = (int) (width/3.5);
    	kCri = 18*lanes;
    	kJam = 125*lanes;
    	qCap = fd.calcQcap(this);
    	KCell = 0;
    	QCell = calcQ(KCell);
    	VCell = calcV(KCell);
    	l = calcLength();
    	
    	
    	indexNodeIn = nodeIn.cellsOut.indexOf(this);
    	indexNodeOut = nodeOut.cellsIn.indexOf(this);
    	
    }
    
    public void setWidth(double w) {
		this.width = w;
	}
	public double getWidth() {
		return width;
	}
	public void setVLim(double sl) {
		this.vLim = sl;
	}
	public double getVLim() {
		return vLim;
	}

	public void addVertex(Vertex vertex) {
		vertices.add(vertex);
	}
	public void addVertex(int i, Vertex vertex) {
		vertices.add(i, vertex);
	}
	public double calcLength() {
		double tmplength = 0;
		for (int i = 0; i<=(vertices.size()-2); i++) {
			tmplength += vertices.get(i).distance(vertices.get(i+1));
		}
		return tmplength;
	}
	
	public double[] calcPointAtDistance(double p) {
		double length = calcLength();
		if (p>length)
			throw new Error("p is larger than distance l");
		
		int i = 0;
		
		double arc = 0;
		double cumlength = 0;
		//System.out.println(p);
		while (cumlength <= p) {
			//System.out.println(cumlength);
			arc = vertices.get(i).distance(vertices.get(i+1));
			//System.out.println(arc);
			cumlength += arc;
			i++;
		}
		double ratio = (p-(cumlength-arc))/arc;
		//System.out.println("ratio:");
		//System.out.println(ratio);
		double result[] = new double[3];
		result[0] = ratio * (vertices.get(i).getX() - vertices.get(i-1).getX()) + vertices.get(i-1).getX();
		result[1] = ratio * (vertices.get(i).getY() - vertices.get(i-1).getY()) + vertices.get(i-1).getY();		
		result[2] = i;
		return result;
		
	}
	public void addIn(Integer i) {
		ins.add(i);
	}
	public void addOut(Integer i) {
		outs.add(i);
	}
	@SuppressWarnings("unchecked")
	public ArrayList<MacroCell> splitInParts(int nrParts) {
		ArrayList<MacroCell> result = new ArrayList<MacroCell>();
		System.out.println("splitCells");
		System.out.println(nrParts);
		if (nrParts == 1 || nrParts == 0) {
			result.add(this);
		} else {
		
			int vert = 0;
			this.l = calcLength();
		for (int i = 0; i< nrParts - 1; i++) {
			
			MacroCell m = new MacroCell(this.model);
			
			m.setWidth(this.width);
			m.setVLim(this.vLim);
			m.setId(new Random().nextInt());
			
			double res[] = this.calcPointAtDistance((i+1)*(this.l)/(nrParts));
			//System.out.println(Arrays.toString(res));
			//System.out.println("vert: " + Integer.toString(vert) + " res: " + Double.toString(res[2]));
			m.vertices = new ArrayList<Vertex>(this.vertices.subList(vert, (int) res[2]));
			vert = (int) res[2];
			m.vertices.add(new Vertex(res[0],res[1],0));
			this.vertices.add(vert, new Vertex(res[0],res[1],0));
		
			
			result.add(m);
		}
		this.vertices = new ArrayList<Vertex>(this.vertices.subList(vert, this.vertices.size()));
	
		
		result.get(0).ups = (ArrayList<MacroCell>) this.ups.clone();
		for (MacroCell c: ups) {
			c.downs.remove(this);
			c.downs.add(result.get(0));
		}
		
			
		
		for(int j=1; j < nrParts -1;j++) {
			//result.get(j-1).downs.clear();
			result.get(j-1).downs.add(result.get(j));
			result.get(j).ups.add(result.get(j-1));
		}
		
		
		result.get(nrParts-2).downs.add(this);
		this.ups.clear();
		this.ups.add(result.get(nrParts-2));
		
		
		result.add(this);
		}
		System.out.println(result.toString());
		return result;
	}
	public String toString() {
		
		
		return "("+this.id+ ", in: "+ this.ups.size()+", out: "+ this.downs.size()+ ")";
	}


	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	

    /** calculate using q given fundamental diagram **/
    public double calcQ(double k) {
    	return fd.calcQ(this);
    	/*if (k<0 || k > kJam)
    		throw new Error ("density is not correct" + Double.toString(k));
    	else if (k<kCri) 
    		*//** triangular FD **//*
    		return k*vLim;
    	else
    		*//** triangular FD **//*
    		return (kJam - k)/(kJam - kCri)*(kCri*vLim);*/
    }
    public double calcV(double k) {
    	return calcQ(k)/k;
    }
    public void updateV() {
    	VCell = calcV(KCell);
    }
    
    public void calcDemand() {
    	if (KCell < kCri)
    		Demand =  calcQ(KCell);
    	else
    		Demand = qCap;
   	
    }
    public void calcSupply() {
    	if (KCell < kCri)
    		Supply =  qCap;
    	else
    		Supply = calcQ(KCell);
    }
    public void calcFluxOut() {
    	/*if (downs.size() == 1) {
    		FluxOut = Math.min(downs.get(0).Supply,Demand);
    		FluxOut2[0] = Math.min(downs.get(0).Supply,Demand);
    	} else if (downs.size() == 0) {
    		FluxOut = Math.min(2000,Demand);
    		FluxOut2[0] = Math.min(qCap,Demand);
    	} else if (downs.size() >= 2){
    		for (int i=0; i<FluxOut2.length; i++) {
    			FluxOut2[i]=Math.min(Demand/FluxOut2.length,downs.get(i).Supply);
    		}
    	
    		
    		FluxOut = Math.min(downs.get(0).Supply,0.5*Demand);
    	} else {
    		throw new Error ("not yet implemented");
    	}*/
    	FluxOut = nodeOut.fluxesIn[indexNodeOut];
    	
    			
    }
    public void calcFluxIn() {
    	/*if (ups.size() == 1) {
    		//FluxIn = Math.min(ups.get(0).Demand,Supply);
    		FluxIn2[0] = Math.min(ups.get(0).Demand,Supply);
    	} else if (ups.size() == 0) {
    		//FluxIn = Math.min(2000,Supply);
    		FluxIn2[0] = Math.min(3000,Supply);
    	} else if (ups.size() >= 2) {
    		FluxIn = Math.min(ups.get(0).Demand,Supply);
    		//System.out.println("Merge Flux In");
    		for (int i=0; i<FluxIn2.length; i++) {
    			//System.out.println(FluxIn2.length);
    			//System.out.println(Supply);
    			double Sstar = (Supply/FluxIn2.length);
    			//System.out.println(Sstar);
    			double S = Sstar;
    			FluxIn2[i] = Math.min(ups.get(i).Demand,S);
    		}
    		
    	} else {
    		throw new Error ("not yet implemented");
    	}*/
    	FluxIn = nodeIn.fluxesOut[indexNodeIn];
    			
    }
    public void updateDensity() {
    	//System.out.println("voor"+Double.toString(KCell));
    	//KCell = KCell + model.dt/l*(FluxIn - FluxOut);
    	/*double sumIn = 0;
    	for (double i: FluxIn2) {
    		sumIn += i;
    	}
    	double FluxInTot = sumIn;
    	
    	double sumOut = 0;
    	for (double j: FluxOut2) {
    		sumOut += j;
    	}
    	double FluxOutTot = sumOut;*/
    	/*System.out.println("Fluxes");
    	System.out.println(FluxInTot);
    	System.out.println(FluxOutTot);*/
    	//System.out.println("Fluxes");
    //System.out.println(FluxIn);
    	//System.out.println(FluxOut);
    	KCell = KCell + model.dt/l*(FluxIn - FluxOut);
    	//System.out.println("na"+Double.toString(KCell));
    	QCell = calcQ(KCell);
    	VCell = calcV(KCell);
    }
    
    /**
     * Sets the lane length based on the x and y coordinates. This method is 
     * called within the constructor and should only be used if coordinates are
     * changed afterwards (for instance to nicely connect lanes at the same 
     * point).
     * @return double; Total length
     */

    
    public void draw(GraphicsPanel graphicsPanel) {
    	//Color color = getDensColor(KCell); 
    	//Color color = getDensColor(new Random().nextInt()); 
    	Color color = getVelocityColor(VCell); 
    	graphicsPanel.setStroke((float) (5+(KCell/125)*15));
		graphicsPanel.setColor(color);
		graphicsPanel.drawPolyLine(vertices);
   	
	}
    public Color getDensColor(double k)
    {
    	double H = 0;
    	if (k<kCri) {
    		H = (kCri-k)/kCri * 0.2+0.1 ;
    	}// Hue (note 0.4 = Green, see huge chart below)
    	else {
    		H = ((kJam-kCri)-(k-kCri))/(kJam-kCri) * 0.1;
    	}// Hue (note 0.4 = Green, see huge chart below)
        double S = 0.9; // Saturation
        double B = 0.9; // Brightness

        return Color.getHSBColor((float) H, (float)S, (float)B);
    }
    public Color getVelocityColor(double v)
    {
    	if (Double.isNaN(v))
    		return Color.black;
    	else {
	    	double H = (v/vLim)*0.4;
	        double S = 0.9; // Saturation
	        double B = 0.9; // Brightness
	
	        return Color.getHSBColor((float) H, (float)S, (float)B);
    	}
    }
    
    
    /**
     * Retrieve the upstream connected MacroCell of this MacroCell.
     * @return MacroCell; the upstream connected MacroCell of this MacroCell
     */
    public java.util.ArrayList<MacroCell> getUps_r() {
    	return ups;
    }
    
    /**
     * Retrieve the downstream connected MacroCell of this MacroCell.
     * @return MacroCell; the downstream connected MacroCell of this MacroCell
     */
    public java.util.ArrayList<MacroCell> getDowns_r() {
    	return downs;
    }
    
    /**
     * Retrieve the left MacroCell of this MacroCell.
     * @return MacroCell; the left MacroCell of this MacroCell
     */
    public MacroCell getLeft_r() {
    	return left;
    }
    
    /**
     * Retrieve the right MacroCell of this MacroCell.
     * @return MacroCell; the right MacroCell of this MacroCell
     */
    public MacroCell getRight_r() {
    	return right;
    }
    
    /**
     * Return the destination of this MacroCell.
     * @return Integer; the destination of this MacroCell, or a negative value if
     * this MacroCell is not a destination
     */
    public int getDestination_r() {
    	return destination;
    }
    
    /**
     * Return the origin of this MacroCell.
     * @return Integer; the origin of this MacroCell or a negative value if this
     * MacroCell is not an origin
     */
    public int getOrigin_r() {
    	return origin;
    }
    
    /**
     * Retrieve the speed limit on this MacroCell.
     * @return Double; the speed limit on this MacroCell in m/s
     */
    public double getSpeedLimit_r() {
    	return vLim;
    }
    
    /**
     * Retrieve the flow of this MacroCell.
     * @return Double; the flow of this MacroCell
     */
    public double getQ_r() {
    	return QCell;
    }
    
    /**
     * Retrieve the density of this MacroCell.
     * @return Double; the density of this MacroCell
     */
    public double getK_r() {
    	return KCell;
    }
    
    /**
     * Retrieve the average speed of this MacroCell.
     * @return Double; the average speed of this MacroCell
     */
    public double getV_r() {
    	return VCell;
    }
    
    
    /**
     * Sets the flow of this MacroCell.
     * @param flow Flow of this cell [veh/h or veh/s].
     */
    public void setQ(double flow) {
        this.QCell = flow;
    }
    
    /**
     * Sets the density of this MacroCell.
     * @param density Density of this cell [veh/km or veh/m].
     */
    public void setK(double density) {
        this.KCell = density;
    }
    
    /**
     * Sets the average speed of this MacroCell.
     * @param speed Speed of this cell [km/h or m/s].
     */
    public void setV(double speed) {
        this.VCell = speed;
    }
    
	/**
     * Returns the ID of the lane.
     * @return ID of the lane.
     */
    public int id() {
        return id;
    }


    /**
     * Returns the speed limit in m/s.
     * @return Speed limit [m/s]
     */
    //public double getVLim() {
    //    return vLim/3.6;
    //}
    void addIn(MacroCell m) {
    	ups.add(m);
    }
    void addOut(MacroCell m) {
    	downs.add(m);
    }
    public String getIns() {
    	String output;
    	if (ups.size() == 0) {
    		output = "()";
    	} else {
    	output = "(";
    	//System.out.println(output);
    	for (MacroCell c: ups) {
    		output = output.concat(Integer.toString(c.id()).concat(","));
    		//System.out.println(output);
    	}
    	output =  output.substring(0, output.length()-1)+")";
    	//System.out.println(output);
    	}
    	return output;
    	
    }
    public String getOuts() {
    	String output;
    	if (downs.size() == 0) {
    		output = "()";
    	} else {
    	output = "(";
    	//System.out.println(output);
    	for (MacroCell c: downs) {
    		output = output.concat(Integer.toString(c.id()).concat(","));
    		//System.out.println(output);
    	}
    	output =  output.substring(0, output.length()-1)+")";
    	//System.out.println(output);
    	}
    	return output;
    	
    }
}
