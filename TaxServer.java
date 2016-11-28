/**
* Course		: SENG3400 Network & Distributed Computing	
* Title	        : SENG3400 Assignment 1
* Name          : TaxServer
* Author        : Alan Nguyen
* Student Number: 3131950
* Due Date      : 31/8/2014
* 
* The TaxServer Program will listen for clients and then
* receive and store tax range data from them. It will also accept 
* income tax queries and calculate income tax payable as requested																
*/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;


/**
 * Class: TaxServer 
 * Creates a serverSocket object and listens for client connections 
 * When a client connects TaxServer creates an new thread for the client
 * and then resumes listening for more clients.
 */
public class TaxServer{
	static Scanner console = new Scanner(System.in);
	static ArrayList<taxRange> taxRangeList = new ArrayList<taxRange>();
	
	public static void main(String[] args) throws Exception{
		int sockPort = 10234;
		System.out.println("Enter a port for server to bind to(default: 10234");
		System.out.println("Leave blank and press enter to leave port as default");
		String portInput = console.nextLine();
		if(portInput.equals(""))
		{
			sockPort = 10234;
		}else if (intValidator(portInput)){
			sockPort = Integer.parseInt(portInput); 
			}else if(!intValidator(portInput))
			{
				System.out.println("Port Entered is not valid Integer.\n Assuming default port. ");
			}
		ServerSocket listenSocket = new ServerSocket(sockPort);
		try{
			while(true){
				System.out.println("Listening for clients...");
				new TaxServerThread(listenSocket.accept()).start();
			}
		}
		finally
		{
			listenSocket.close();
		}
		
	}
	/*this method takes a string and returns true if the string is a valid integer
	 * otherwise it returns false;
	 * */
	static boolean intValidator(String testString){
		boolean isInteger=false;
		try{
			int testInt = Integer.parseInt(testString);
			isInteger = true;
		}catch(NumberFormatException e){
			isInteger = false;
		}
		return isInteger;
	}
	
	static class TaxServerThread extends Thread{
		Socket taxServerSocket;
		OutputStream outStream;
		InputStream in;
		Writer outputWrite;
		BufferedReader readIn;
		boolean runFlag;
		/* taxServerThread constructor*/
		public TaxServerThread(Socket listenSocket){
			
			try{
				this.taxServerSocket = listenSocket;
				this.outStream = this.taxServerSocket.getOutputStream();
				this.outputWrite= new OutputStreamWriter(outStream, "US-ASCII");
				this.in = this.taxServerSocket.getInputStream();
				this.readIn = new BufferedReader(new InputStreamReader(this.in,"US-ASCII"));
				this.runFlag=true;																							
			}catch(IOException e){
				System.err.println("Error");
				}
			}
		/*
		 *  This is the threads run message. It is the method that is called
		 *  when the TaxServer class creates a new thread. Its main purpose here
		 *  is to validate the initial message from the client and either call the 
		 *  main loop or close the connection with the client.
		 * 
		 * */
		public void run(){
			try{
				/*tests to see if clients first Message is TAX. */
				String taxInput = this.readIn.readLine(); 
				System.out.println(taxInput);
				if (taxInput.equals("TAX")){
					sendMessage("TAX: OK\n");
					System.out.println("TAX: OK");
				}else{
					this.runFlag =false; //If first message isn't TAX then server doesn't make it to the main loop, thus shuts down
				}
				while(this.runFlag)
				{
					mainLoop(); /*Mainloop runs while Taxserver runflag is true*/
				}
			}catch(IOException e){
				System.err.println("IO Error");
			}finally{
				try {
					this.taxServerSocket.close();
				} catch (IOException e) {
					System.err.println("Socket did not close properly.");
				}
			}
			
		}
		/**
		 * Takes a string and outputs to the client
		 * Flushs the stream after sending message.
		 * @param output
		 */
		void sendMessage(String output){
			try{
				this.outputWrite.write(output);
				this.outputWrite.flush();
			}catch(IOException e){
				System.err.println("Failed to send message.");
			}
		}
		/**
		 * Receives 4 string inputs from the client program and stores them 
		 * within an ArrayList called taxRangeList. This method will make sure
		 * there are no intersections between the ranges.  
		 */
		 
		synchronized void storeRanges(){
			boolean isValid = true; //set to false if any of the inputs are not valid integers.
			int testInt = 0; //used to test if tempLowRange and tempHighRange are integers
			String tempLowRange=null;
			String tempHighRange=null;
			int tempBaseTax=0;
			int tempTaxPerDollar=0;		
			try{
				/*reading input from client*/
				tempLowRange = this.readIn.readLine();
				testInt = Integer.parseInt(tempLowRange);
				tempHighRange = this.readIn.readLine();
				//firsts tests if tempHighRange is unbounded character then if false tests if Integer
				if(!tempHighRange.equals("~")){
					testInt = Integer.parseInt(tempHighRange);
					if(Integer.parseInt(tempLowRange)>testInt){
						System.out.println("Beginning value is greater than ending value");
						System.out.println("Setting ending value to ~");
						tempHighRange = "~";
					}
				}
				tempBaseTax = Integer.parseInt(this.readIn.readLine());
				tempTaxPerDollar = Integer.parseInt(this.readIn.readLine());
			}catch(IOException e){
				System.err.println("Input not valid: IOERROR. Integer Expected");
				isValid = false;
			}
			saveRange(tempLowRange,tempHighRange,tempBaseTax,tempTaxPerDollar);
			System.out.println("CLIENT: "+tempLowRange+" "+tempHighRange+" "+tempBaseTax+" "+tempTaxPerDollar);
			if(isValid){
				sendMessage("STORE: OK\n");
				System.out.println("STORE: OK");
			}
		}
		
		/**
		 * This method is used by the storeRanges() method to store the input inside the taxRangesList ArrayList
		 * 
		 * @param tempLowRange
		 * @param tempHighRange
		 * @param tempBaseTax
		 * @param tempTaxPerDollar
		 */
		synchronized void saveRange(String tempLowRange,String tempHighRange,int tempBaseTax, int tempTaxPerDollar){
			
			String previousHighRange=null; //previousHighRange and nextLowRange will be used to store the values
			String nextLowRange=null; 	   //used to edit intersections between the different tax ranges
			int newRangeIndex = 0;         //this int is used to keep track of where the new Range is being stored
			/*creates a new taxRange if the arraylist is empty*/
			if(taxRangeList.isEmpty()){
				taxRangeList.add(new taxRange(tempLowRange,tempHighRange,tempBaseTax,tempTaxPerDollar));
			/*case when there is one or more range inside the arraylist*/
			}else if(taxRangeList.size()>0){
				//finds proper position to add new arraylist
				boolean addedFlag = false; //flag set to true if for loop finds a place to add range
				
				
				// If the input has an unbounded ranged this if statement will set the delete flag
				// for all nodes that have a lower beginning value than the new node	
				if(tempHighRange.equals("~")){
					for(int i=0;i<taxRangeList.size();i++){
							if(Integer.parseInt(tempLowRange)<=taxRangeList.get(i).lowValueInt()){
								taxRangeList.get(i).deleteFlag=true;
								}
					}
				}
				// For loops checks the ranges in the list to see if there is a place not at the end of list to place
				// the new range. This is done by comparing all the beginning values of the tax ranges to the beginning
				// value of the new tax Range. if two ranges have the same beginning value then the new range will be placed in front
				for(int i=0;i<taxRangeList.size();i++){
					if(Integer.parseInt(tempLowRange)<=taxRangeList.get(i).lowValueInt()){
						addedFlag = true;
						newRangeIndex = i;
						taxRangeList.add(i,new taxRange(tempLowRange,tempHighRange,tempBaseTax,tempTaxPerDollar));
						break;
						}
					}
				// If the previous loop can't find a place the new node is placed at the end of the list
				if(!addedFlag){	
					taxRangeList.add(new taxRange(tempLowRange,tempHighRange,tempBaseTax,tempTaxPerDollar));
					newRangeIndex = taxRangeList.size()-1;
					
					} 
				// This part of the code will check if there are any intersections between the range of tax values
				if(newRangeIndex!=0){
					int i=newRangeIndex-1; //index of the taxRange previous to the new taxRange
					
					//checking if the new ranges beginning value is less than the previous ranges high value 
					if(taxRangeList.get(newRangeIndex).lowValueInt()<=taxRangeList.get(i).highValueInt()){
						//checks if new range falls between previous range and adds a new range with appropriate values
						// lowValue = new ranges ending value+1 , highvalue = previous range high value, previous tax values
						if(taxRangeList.get(i).highValueInt()>taxRangeList.get(newRangeIndex).highValueInt()){
							taxRangeList.add((newRangeIndex+1),new taxRange(
											Integer.toString(taxRangeList.get(newRangeIndex).highValueInt()+1),
											taxRangeList.get(i).highValue,taxRangeList.get(i).baseTax,
											taxRangeList.get(i).taxPerDollar));
							}
						//deals with the case that happens when new range falls in between an unbounded range as previous code
						//will allow two unbounded ranges
						if(taxRangeList.get(newRangeIndex).highValue.equals("~")){
							taxRangeList.remove(newRangeIndex+1);
						}
						//changes the previous ranges high value to he correct value. new range low range -1
						taxRangeList.get(i).highValue=Integer.toString((taxRangeList.get(newRangeIndex).lowValueInt()-1));
						}	
					}
				//checks the ranges to the right of new range for intersections	
				if(taxRangeList.size()>1){
					for(int i=newRangeIndex+1;i<taxRangeList.size();i++){
						//first case to check is if the range to the right is unbounded
						if(taxRangeList.get(i).highValue.equals("~")){
							//will break and do nothing if no intersection
							if(taxRangeList.get(i).lowValueInt()>taxRangeList.get(newRangeIndex).highValueInt()){
								break;
								}else if(taxRangeList.get(i).lowValueInt()<taxRangeList.get(newRangeIndex).highValueInt()){										taxRangeList.get(i).lowValue=Integer.toString(taxRangeList.get(newRangeIndex).highValueInt()+1);
									break;
								}
								//checks if range to the rights high value is higher than new ranges high value
							}else if(taxRangeList.get(i).highValueInt()>taxRangeList.get(newRangeIndex).highValueInt()){
									//do nothing if rights low value is also higher than new range high value- no intersection
									if(taxRangeList.get(i).lowValueInt()>taxRangeList.get(newRangeIndex).highValueInt()){
										break;
									}else{
										//adjusts right ranges low value to correct value is it is lower than new ranges high value
										taxRangeList.get(i).lowValue=Integer.toString(taxRangeList.get(newRangeIndex).highValueInt()+1);
										break;	
										}
									}
						    //flags the range for deletion if right ranges high range is less than or equal to the new ranges high value
							if(taxRangeList.get(i).highValueInt()<=taxRangeList.get(newRangeIndex).highValueInt()){
										taxRangeList.get(i).deleteFlag=true;
							}
					}
					// Removes all the ranges that have been flagged for deletion	
					for(int i=taxRangeList.size()-1;i>0;i--){
						if(taxRangeList.get(i).deleteFlag){
							taxRangeList.remove(i);
							}
						}
				}
				//will deal with cases where existing ranges already have an unbounded range
				try{
					//occurs when an unbounded range gets inserted when theres an existing unbounded range 
					//with a lower beggining value.  eg: existing 6000-~ : new 7000-~
					if(taxRangeList.get(newRangeIndex).highValue.equals("~")){
						if(taxRangeList.get(newRangeIndex-1).highValue.equals("~")){
							//sets existing range with unbounded range to higher range lower value -1
							taxRangeList.get(newRangeIndex-1).highValue=Integer.toString(Integer.parseInt(tempLowRange)-1);
						}
					}
					//deals with intersecting unbounded ranges. creates an extra taxrange to deal with this case.
					if(taxRangeList.get(newRangeIndex-1).highValue.equals("~")){
						if(taxRangeList.get(newRangeIndex).lowValueInt()>taxRangeList.get(newRangeIndex-1).lowValueInt()
						&&(!taxRangeList.get(newRangeIndex).highValue.equals("~"))){
							taxRangeList.get(newRangeIndex-1).highValue=Integer.toString(taxRangeList.get(newRangeIndex).lowValueInt()-1);
							taxRangeList.add(new taxRange(Integer.toString(taxRangeList.get(newRangeIndex).highValueInt()+1),"~",
							taxRangeList.get(newRangeIndex-1).baseTax,taxRangeList.get(newRangeIndex-1).taxPerDollar));
							} 
						}
					}catch(ArrayIndexOutOfBoundsException e){
							//empty catch
					}									
			}
		}
		
		/*
		 * This method is called when QUERY message is received from client.
		 * Outputs all the tax details from the taxRangesList
		 * */		
		void query(){
			if(taxRangeList.size()!=0){
				for(int i=0;i<taxRangeList.size();i++){
					taxRange temp = taxRangeList.get(i);
					String outputMessage = temp.lowValue+" "+temp.highValue+" " + temp.baseTax +" "+temp.taxPerDollar+"\n";
					System.out.println("SERVER: "+outputMessage);
					sendMessage(outputMessage);

				}
			}
			sendMessage("QUERY: OK\n");
			System.out.println("QUERY: OK");
		}
		/* Takes the client input and returns income tax payable depending on value passed.
		 * @param clientInput
		 * */
		void incomeTaxOutput(String clientInput){
			try{
				boolean sentTaxOut = false; //set to true when this method sends an income tax amount to client
				boolean isInteger = intValidator(clientInput); //checks to see if input is integer			
				int clientInputInt=Integer.parseInt(clientInput);//converts the clientInput to an integer to be used for calulcation
				if(!isInteger)
					//returns this message if input isnt integer
					sendMessage("I DONT KNOW "+clientInput+"\n");
					System.out.println("I DONT KNOW "+clientInput);
				if(isInteger){
					//loop checks all the tax ranges to see if it can calculate a tax based on the ranges
					//the server knows and sends the value to the client. flag sentTaxOut is set to true if value found
					for(int i=0;i<taxRangeList.size();i++){
						if(taxRangeList.get(i).highValue.equals("~")){
							sendMessage("TAX IS "+(double)(taxRangeList.get(i).getIncomeTax(clientInputInt))+"\n");
							System.out.println("SERVER: TAX IS "+(double)(taxRangeList.get(i).getIncomeTax(clientInputInt)));
							sentTaxOut = true;
						}else if(clientInputInt<=taxRangeList.get(i).highValueInt()&&clientInputInt>=taxRangeList.get(i).lowValueInt()){
							sendMessage("TAX IS "+(double)(taxRangeList.get(i).getIncomeTax(clientInputInt))+"\n");
							System.out.println("SERVER: TAX IS "+(double)(taxRangeList.get(i).getIncomeTax(clientInputInt)));
							sentTaxOut = true;
						}
						if(sentTaxOut)
							break;
					}
					//sends an i dont know message if server cant calculate an income tax
					if(!sentTaxOut)
						sendMessage("I DONT KNOW "+clientInput+"\n");
						System.out.println("SERVER: I DONT KNOW "+clientInput);
				}
			}catch(NumberFormatException e){
				sendMessage("I DONT KNOW "+clientInput+"\n");
				System.out.println("SERVER: I DONT KNOW "+clientInput);
			}
		}
		
		/**
		 * mainLoop waits for a message from the client program and then switches into the appropriate method
		 * depending on the message received. mainLoop will loop until it receives an end command from client
		 * 
		 **/
		void mainLoop(){
			try{
				String clientInput = this.readIn.readLine(); //client input. used to switch to appropriate method
				System.out.println("CLIENT: "+clientInput);
				switch(clientInput){
					case "STORE":
								storeRanges();
								break;
					case "QUERY":
								query();
								break;
					case "BYE":
								sendMessage("BYE: OK\n");
								this.runFlag=false; 
								break;
					case "END":
								sendMessage("END: OK\n");
								System.exit(0);
								break;
					default:
								incomeTaxOutput(clientInput);
								break;
					}
				}catch(IOException e){
					System.err.println("error");
					runFlag = false;
				}	
			}
	}
	
}
/**
 * The taxRange class stores the values of each tax range
 */
					
class taxRange{
	String lowValue;
	String highValue;
	int baseTax;
	int taxPerDollar;
	boolean deleteFlag; //when flag is set to true the save range methods knows to delete the 
						//particular range

	
	taxRange(String lowValue,String highValue,int baseTax,int taxPerDollar){
		this.lowValue=lowValue;
		this.highValue=highValue;
		this.baseTax=baseTax;
		this.taxPerDollar=taxPerDollar;
		this.deleteFlag=false;
	}
	/**
	 *calculate income tax based on the range values stored in the current range and returns
	 * @param income
	 * @return incomeTax
	 */
	double getIncomeTax(int income){
		double incomeTax =this.baseTax+(income - (this.lowValueInt()))*taxPerDollar/100.00;
		return incomeTax;
	}
	/**
	 * Returns the integer value of the beginning range
	 * @return lowValueInt
	 **/
	int lowValueInt(){
		int lowValueInt = Integer.parseInt(this.lowValue);
		return lowValueInt;
	}
	/**
	 * Returns the integer value of the ending range if not ~
	 * otherwise it returns maxint value if range is unbounded.
	 * @return highValueInt
	 **/
	int highValueInt(){
		int highValueInt;
		if (this.highValue.equals("~")){
			highValueInt=-1;
		}else{
			highValueInt = Integer.parseInt(this.highValue);
		}
		return highValueInt;
	}
}

