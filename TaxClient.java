/**
* Course		: SENG3400 Network & Distributed Computing	
* Title	        : SENG3400 Assignment 1
* Name          : TaxCLient
* Author        : Alan Nguyen
* Student Number: 3131950
* Due Date      : 31/8/2014
 *
 * The TaxClient Program will connect to a TaxServer Program.
 * It will send tax ranges that will be stored on the server 
 * and will be able to query for what ranges are stored on the 
 * server and ask the server to calculate tax payable on incomes.
 * 
 **/


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream ;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 * TaxClient class connects to a tax server program and manages the input
 * and output.
 * 
 */ 
public class TaxClient {
	
	Socket clientSocket;	 
	OutputStream outStream;
	InputStream in;
	Writer outputWrite;
	BufferedReader readIn;
	static Scanner console = new Scanner(System.in);
	
	
	/* Constructor for TaxClient class */
	/* */
	TaxClient(InetAddress serverIp,int serverPort){
		try {
			this.clientSocket = new Socket();
			this.clientSocket.connect(new InetSocketAddress(serverIp,serverPort),10000);	
			this.clientSocket.setSoTimeout(3000);
			this.outStream = this.clientSocket.getOutputStream();
			this.outputWrite = new OutputStreamWriter(outStream, "US-ASCII");
			this.in = this.clientSocket.getInputStream();
			this.readIn = new BufferedReader(new InputStreamReader(this.in,"US-ASCII"));
		} catch (IOException e) {
			System.err.println("Could not connect to "+serverIp+":"+serverPort);
		} 
	}
	/**
	 * Run method will wait for the user to input a command. run will then
	 * switch into the appropriate method depending on the input.
	 * Run method  will loop while runflag is true.
	 **/
	void run() {
		boolean runFlag = true;
		defaultMessageCommand("TAX");
		while(runFlag){
			System.out.println("Enter STORE, QUERY, BYE and END or an income to test ");
			String input = console.nextLine();
			input = input.toUpperCase();
			switch(input){
				case "STORE": storeCommand(input); break;
				case "QUERY": queryCommand(input); break;
				case "BYE"  : runFlag = false;						//breakthrough intentional
				case "END"  : runFlag = false;					    //bye and end will cause runFlag to be false causing the client to terminate
				default:defaultMessageCommand(input); break;
			}
		}
	}
	/**
	 * storeCommand will ask the user for a starting value, ending value
	 * base tax and tax per dollar. It will then send those values to the
	 * tax server to be stored
	 * @param input  
	 * 
	 **/
	void storeCommand(String input){
		String outputString = null;              //string to be outputted to taxServer;
		String lowRange=null;					 //beginning range
		String highRange=null;;                  //ending range
		String baseTax;							 //base tax
 		String taxPerDollar;                     //tax per dollar
	
		//User Inputs
	    System.out.println("Enter Starting/Low Range income value :");
		lowRange = console.nextLine();
		System.out.println("Enter Ending/High range income value");
		highRange = console.nextLine();
		System.out.println("Enter base Tax :");
		baseTax = console.nextLine();
		System.out.println("Enter tax per Dollar (in cents):");
		taxPerDollar = console.nextLine();
		//send string to server
		outputString = "STORE\n"+lowRange+"\n"+highRange+"\n"+baseTax+"\n"+taxPerDollar+"\n";
		try{
			this.outputWrite.write(outputString);
			this.outputWrite.flush();
			System.out.println(this.readIn.readLine());
		}catch(IOException e){
			System.err.println("Error sending message");
		}
		
	}
	
	/**
	 * This method sends the message "QUERY" to the server and then 
	 * receives all the tax ranges that are stored on the server if any
	 * until it receives 
	 **/
	void queryCommand(String input){
		String line = null;
		try {
			this.outputWrite.write(input.toUpperCase()+"\n");
			this.outputWrite.flush();
			boolean streamBool = true;
			//query command will expect and line from the server one at a time until it receives query ok
			while(streamBool)
			{
				line = this.readIn.readLine();
				System.out.println(line);
				if(line.contains("OK"))
					streamBool = false;
			}
		} catch (IOException e) {
			System.err.println("Input/Output error");
		}
		
	}
	/**
	 * defaultmessageCommand will send whatever input its given
	 * and send it to the taxserver. it will then wait for a response
	 * @param input
	 * 
	 **/ 
	void defaultMessageCommand(String input){
		try {
			this.outputWrite.write(input.toUpperCase()+"\n");
			this.outputWrite.flush();
			System.out.println(this.readIn.readLine());
			}
		 catch(IOException e) {
			System.err.println("Input/Output Error");
		 }
	}
	/**
	 * Takes a string and checks if the string is a valid integer
	 * will print out errorMessage if not valid integer.
	 * @param outString
	 * @param errorMessage
	 * @return stringValid
	 **/
	static String intValidator(String outString, String errorMessage){
		String validString = null; 
		boolean stringValid = true;  
		int testInt = 0;
		try{
			testInt = Integer.parseInt(outString);
			}catch(NumberFormatException e){
				stringValid = false;
				}      
		if (stringValid){
			validString = String.valueOf(testInt);
			} else {
				System.out.println(errorMessage);
				validString = intValidator(console.nextLine(),errorMessage);
				}
		return validString;
	}
	
	
	/**
	 * Main Method 
	 * Asks the user for a server IP and port.
	 * Creates a new TaxClient object to with socket details. 
	 **/
	public static void main(String[] args){
		boolean socketDetails = false;
		InetAddress serverIP=null;
		
		int serverPort = 10234;
		//User Input for Server IP
			try {
				System.out.println("Enter the IP address of the server(default localhost)");
				System.out.println("Leave blank and press enter to connect to default");
				String serverInput = console.nextLine();
				if(serverInput.equals("")){
					serverIP = InetAddress.getLocalHost();
				}else {
					serverIP = InetAddress.getByName(serverInput);
				}
				System.out.println("Enter the port of the server (default port 10234)");
				System.out.println("Leave blank and press enter to connect to default");
				String portInput = console.nextLine();
				if(portInput.equals("")){serverPort = 10234;}else{
					serverPort = Integer.parseInt(intValidator(portInput,"invalid port: enter another :"));
					}
			} catch (UnknownHostException e) {  
				System.err.println("Error");
			
		}
		// creates a new tax client object and then runs it.
		TaxClient userTaxClient = new TaxClient(serverIP,serverPort);
		try{
			userTaxClient.run();
			userTaxClient.clientSocket.close();
		}catch(NullPointerException e){
			System.err.println("Client failed to connect to server.");
		}catch(IOException e){
			System.err.println("Could not close Socket");}
		
		System.out.println("Client shutting down...");
		
	}
}



