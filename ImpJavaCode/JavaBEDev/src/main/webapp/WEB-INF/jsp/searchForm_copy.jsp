<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Insert title here</title>
<script type="text/javascript" src="http://code.jquery.com/jquery-latest.min.js"></script>

<script type="text/javascript"> 

$(document).ready(function() {
	$("#multiDiv").hide();
	  $("#Multicity").click(function() {
	 	    $("#multiDiv").show();
	  });
	  $("#OneWay").click(function() {
	 	    $("#multiDiv").hide();
	  });
	  $("#Return").click(function() {
	 	    $("#multiDiv").hide();
	  });
	  $("#FlightHotel").click(function() {
	 	    $("#multiDiv").hide();
	  });
	});
	
function disableTextBox()
{
	
	if(document.getElementById('OneWay').checked || document.getElementById('Multicity').checked || document.getElementById('FlightHotel').checked)
 	 {
		
 	        document.getElementById('datepicker2').disabled=true; 
 	   }else{
 	                    document.getElementById('datepicker2').disabled = false;
 	                }
 }

</script>

</head>
<body>

	<form method="post" action="getDetails">
	
		<div style="background-color: blue; color: white; padding: 10px;">
			<div style="background-color: yellow; color: black; padding: 20px;">

				<input type="radio" name="tripType" id="OneWay" value="OneWay" onclick="disableTextBox()" >OneWay
				<input type="radio" name="tripType" id="Return" value="Return" onclick="disableTextBox()" checked>Return 
				<input type="radio" name="tripType" id="Multicity" value="Multicity" onclick="disableTextBox()" > Multicity 
				<input type="radio" name="tripType" id="FlightHotel" value="FlightHotel" onclick="disableTextBox()">Flight+Hotel
			</div>
			<p></p>
			<div id='div1'>
				From: <input type="text" id="originLocation" name="originLocation"  size="8" />
				To: <input type="text" id="destinationLocation" name="destinationLocation" size="8"/> 
				Departure Date(dd/mm/yy):<input type="text" id="datepicker1" name="departureDate" size="8" /> 
				Return Date(dd/mm/yy):<input type="text" id="datepicker2" name="arrivalDate" size="8"/>
			</div>
			<p></p>
			<div id='multiDiv' >
				From: <input type="text" id="originLocation1" name="originLocation1" size="8"/>
				To: <input type="text" id="destinationLocation1"	name="destinationLocation1" size="8" /> 
				Departure Date(dd/mm/yy):<input type="text" id="datepicker1" name="departureDate1" size="8"/> 
			</div>
			<p></p>
			<div id="divPass">
				Adult:<input	type="text" id="adult" name="adult" size="8" /> 
				Children:<input	type="text" id="children" name="children" size="8"  /> 
				Infant:<input	type="text" id="infant" name="infant" size="8"  "/> 
				Senior citizen: <input type="text" name="senior" size="8">
			</div>
			<p></p>
			<div id="cabin">
				Class:<select id="cabin" name="cabinClass"><option>Business</option><option>Economy</option></select> 
				<input type="submit" name="search" value="Search">
			</div>

		</div>
 			<input type="hidden" id="transactionID" name="transactionID" value="12345">
 			<input type="hidden" id="sessionID" name="sessionID" value="SID101">
 			<input type="hidden" id="userID" name="userID" value="UID101">
 			<input type="hidden" id="clientType" name="clientType" value="B2B">
 			<input type="hidden" id="clientID" name="clientID" value="B2B101">
	</form>


</body>
</html>