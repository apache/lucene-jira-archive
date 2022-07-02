<%
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
response.setDateHeader("Expires", 0); //prevents caching at the proxy server
response.setHeader("Cache-Control", "private"); // HTTP 1.1 
response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 
response.setHeader("Cache-Control", "max-stale=0"); // HTTP 1.1 
%>
<%/* --------------------------------------------------------------------------------------------
 Hurix Systems Pvt. Ltd.

 Version Information :
 
 Version Number		    : 1.00
 Coded by				: Balaji Kumar
 Release Date			: 
 
 -------------------------------- Description ---------------------------------------------------
This jsp is used to display Register first Login page. 
 ---------- Change Log -------------------------------------------------------------------------
 Version			:
 Name			:
 Date				:
 Description	:
 -------------------------------------------------------------------------------------------- */%><!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ taglib prefix="spring" uri="/spring"%>
<%@ taglib prefix="fmt" uri="/tld/fmt.tld"%>
<%@ taglib prefix="form" uri="/tld/spring-form.tld"%>

<html xmlns="http://www.w3.org/1999/xhtml">
	<%@ page import="com.mgh.sps.common.constants.app.AppConstants"%>
	<%@ page import="java.util.*,com.mgh.sps.common.dto.UserDTO" %>
	
	<head>
		<meta http-equiv="Content-Type"
			content="text/html; charset=iso-8859-1" />
		<title>GradeGuru, note sharing by students for students</title>
		<link href="css/styles.css" rel="stylesheet" type="text/css" />
<!-- CODE ADDED FOR STUBBING --->
<script type="text/javascript" src="js/addEvent.js"></script>
<script type="text/javascript" src="js/sweetTitles.js"></script>
<!-- CODE ENDED FOR STUBBING --->


<style type="text/css">
	/* Big box with list of options */
	#ajax_listOfOptions{
		position:absolute;	/* Never change this one */
		width:619px;	/* Width of box */
		height:200px;	/* Height of box */
		overflow:auto;	/* Scrolling features */
		border:1px solid #317082;	/* Dark green border */
		background-color:#FFF;	/* White background color */
		text-align:left;
		font-size:0.9em;
		z-index:100;
	}
	#ajax_listOfOptions div{	/* General rule for both .optionDiv and .optionDivSelected */
		margin:1px;		
		padding:1px;
		cursor:pointer;
		font-size:0.9em;
	}
	#ajax_listOfOptions .optionDiv{	/* Div for each item in list */
		
	}
	#ajax_listOfOptions .optionDivSelected{ /* Selected item in the list */
		background-color:#3399CC;
		color:#FFF;
	}
	#ajax_listOfOptions_iframe{
		background-color:#F00;
		position:absolute;
		z-index:5;
	}
	
	form{
		display:inline;
	}
</style>

<script type="text/javascript">
//******************* ajax code for auto completion of university name*********************************************************************
	var browser_find;
	var mac_ver;
	browser_version= parseInt(navigator.appVersion);
	browser_type = navigator.appName;
	if (navigator.appVersion.indexOf("PPC Mac OS X")!=-1 || navigator.appVersion.indexOf("Intel Mac OS X")!=-1){
			if (-1 != navigator.userAgent.indexOf("Safari") && navigator.appVersion.indexOf("PPC Mac OS X")!=-1){
					browser_find="macsafari";
					mac_ver = "PPC";
				}
			if (-1 != navigator.userAgent.indexOf("Safari") && navigator.appVersion.indexOf("Intel Mac OS X")!=-1){
					browser_find="macsafari";
					mac_ver = "Intel";
				}
		}else{
			if (navigator.appVersion.indexOf("MSIE")!=-1){
					browser_find="iewin";
			}else if (-1 != navigator.userAgent.indexOf("Safari")){
					browser_find="safari";
			}else if (-1 != navigator.userAgent.indexOf("Mozilla")){ 
					if(navigator.appVersion.indexOf("Windows")!=-1)
					{
						browser_find="mozilla";
					}else{
						browser_find="mac_mozilla";	
					}
			}else{
					browser_find="other";
			}
		}
</script>

<script type="text/javascript">
if(browser_find == "mozilla"){	
	document.write('<style type="text/css"> #ajax_listOfOptions{ margin-top:10px; margin-left:128px; } </style>');
}
if(browser_find == "mac_mozilla"){	
	document.write('<style type="text/css"> #ajax_listOfOptions{ margin-top:10px; margin-left:128px; } </style>');
}
if(browser_find == "macsafari" && mac_ver == "PPC"){	
	document.write('<style type="text/css"> #ajax_listOfOptions{ margin-top:504px; margin-left:127px; } </style>');
}
if(browser_find == "macsafari" && mac_ver == "Intel"){	
	document.write('<style type="text/css"> #ajax_listOfOptions{ margin-top:504px; margin-left:140px; } </style>');
}
</script>

<script type="text/javascript">

function sack(file) {
	this.xmlhttp = null;

	this.resetData = function() {
		this.method = "POST";
  		this.queryStringSeparator = "?";
		this.argumentSeparator = "&";
		this.URLString = "";
		this.encodeURIString = true;
  		this.execute = false;
  		this.element = null;
		this.elementObj = null;
		this.requestFile = file;
		this.vars = new Object();
		this.responseStatus = new Array(2);
  	};

	this.resetFunctions = function() {
  		this.onLoading = function() { };
  		this.onLoaded = function() { };
  		this.onInteractive = function() { };
  		this.onCompletion = function() { };
  		this.onError = function() { };
		this.onFail = function() { };
	};

	this.reset = function() {
		this.resetFunctions();
		this.resetData();
	};

	this.createAJAX = function() {
		try {
			this.xmlhttp = new ActiveXObject("Msxml2.XMLHTTP");
		} catch (e1) {
			try {
				this.xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");
			} catch (e2) {
				this.xmlhttp = null;
			}
		}

		if (! this.xmlhttp) {
			if (typeof XMLHttpRequest != "undefined") {
				this.xmlhttp = new XMLHttpRequest();
			} else {
				this.failed = true;
			}
		}
	};

	this.setVar = function(name, value){
		this.vars[name] = Array(value, false);
	};

	this.encVar = function(name, value, returnvars) {
		if (true == returnvars) {
			return Array(encodeURIComponent(name), encodeURIComponent(value));
		} else {
			this.vars[encodeURIComponent(name)] = Array(encodeURIComponent(value), true);
		}
	}

	this.processURLString = function(string, encode) {
		encoded = encodeURIComponent(this.argumentSeparator);
		regexp = new RegExp(this.argumentSeparator + "|" + encoded);
		varArray = string.split(regexp);
		for (i = 0; i < varArray.length; i++){
			urlVars = varArray[i].split("=");
			if (true == encode){
				this.encVar(urlVars[0], urlVars[1]);
			} else {
				this.setVar(urlVars[0], urlVars[1]);
			}
		}
	}

	this.createURLString = function(urlstring) {
		if (this.encodeURIString && this.URLString.length) {
			this.processURLString(this.URLString, true);
		}

		if (urlstring) {
			if (this.URLString.length) {
				this.URLString += this.argumentSeparator + urlstring;
			} else {
				this.URLString = urlstring;
			}
		}

		// prevents caching of URLString
		this.setVar("rndval", new Date().getTime());

		urlstringtemp = new Array();
		for (key in this.vars) {
			if (false == this.vars[key][1] && true == this.encodeURIString) {
				encoded = this.encVar(key, this.vars[key][0], true);
				delete this.vars[key];
				this.vars[encoded[0]] = Array(encoded[1], true);
				key = encoded[0];
			}

			urlstringtemp[urlstringtemp.length] = key + "=" + this.vars[key][0];
		}
		if (urlstring){
			this.URLString += this.argumentSeparator + urlstringtemp.join(this.argumentSeparator);
		} else {
			this.URLString += urlstringtemp.join(this.argumentSeparator);
		}
	}

	this.runResponse = function() {
		eval(this.response);
	}

	this.runAJAX = function(urlstring) {
		if (this.failed) {
			this.onFail();
		} else {
			this.createURLString(urlstring);	
			
			if (this.element) {
				this.elementObj = document.getElementById(this.element);			
			}
			if (this.xmlhttp) {
				var self = this;
				
				if (this.method == "GET") {
					totalurlstring = this.requestFile + this.queryStringSeparator + this.URLString;
					this.xmlhttp.open(this.method, totalurlstring, true);
				} else {
					this.xmlhttp.open(this.method, this.requestFile, true);
					try {						
						this.xmlhttp.setRequestHeader("Content-Type", "application/x-www-form-urlencoded")
					} catch (e) { }
				}
				
				this.xmlhttp.onreadystatechange = function() {
					switch (self.xmlhttp.readyState) {
						case 1:
							self.onLoading();							
							break;
						case 2:
							self.onLoaded();							
							break;
						case 3:
							self.onInteractive();
							break;
						case 4:
							self.response = self.xmlhttp.responseText;
							self.responseXML = self.xmlhttp.responseXML;
							self.responseStatus[0] = self.xmlhttp.status;
							self.responseStatus[1] = self.xmlhttp.statusText;
							
							if (self.execute) {
								self.runResponse();
							}
							
							//alert(self.response);
																					
							if (self.elementObj) {					
								elemNodeName = self.elementObj.nodeName;
								elemNodeName.toLowerCase();
								if (elemNodeName == "input"
								|| elemNodeName == "select"
								|| elemNodeName == "option"
								|| elemNodeName == "textarea") {									
									self.elementObj.value = self.response;
								} else {
								  self.elementObj.innerHTML = self.response;
								}
							}
							
							if (self.responseStatus[0] == "200") {								
								self.onCompletion();
							} else {
								self.onError();
							}

							self.URLString = "";
							break;
					}
				};

				this.xmlhttp.send(this.URLString);
			}
		}
	};

	this.reset();
	this.createAJAX();
}

</script>




<script type="text/javascript">
	var ajaxBox_offsetX = 0;
	var ajaxBox_offsetY = 0;
	var ajax_list_externalFile = 'retriveCourses.htm';	// Path to external file
	var minimumLettersBeforeLookup = 3;	// Number of letters entered before a lookup is performed.
	var ajax_list_objects = new Array();
	var ajax_list_cachedLists = new Array();
	var ajax_list_activeInput = false;
	var ajax_list_activeItem;
	var ajax_list_optionDivFirstItem = false;
	var ajax_list_currentLetters = new Array();
	var ajax_optionDiv = false;
	var ajax_optionDiv_iframe = false;
	var sel_option = "NONE";
	var p_obtain_id = 0;
	var ajax_list_MSIE = false;
	if(navigator.userAgent.indexOf('MSIE')>=0 && navigator.userAgent.indexOf('Opera')<0)ajax_list_MSIE=true;
	
	var currentListIndex = 0;
	
	function ajax_getTopPos(inputObj)
	{
		
	  var returnValue = inputObj.offsetTop;
	  while((inputObj = inputObj.offsetParent) != null){
	  	returnValue += inputObj.offsetTop;
	  }
	  return returnValue;
	}
	function ajax_list_cancelEvent()
	{
		return false;
	}
	
	function ajax_getLeftPos(inputObj)
	{
	  var returnValue = inputObj.offsetLeft;
	  while((inputObj = inputObj.offsetParent) != null)returnValue += inputObj.offsetLeft;
	  
	  return returnValue;
	}
	
	function ajax_option_setValue(e,inputObj)
	{		
		if(!inputObj)inputObj=this;
		var tmpValue = inputObj.innerHTML;
		if(ajax_list_MSIE)tmpValue = inputObj.innerText;else tmpValue = inputObj.textContent;
		if(!tmpValue)tmpValue = inputObj.innerHTML;
		if(browser_find == "macsafari"){
			var m_obtain_name_one = tmpValue.replace(/<uName>/i,"");
			var m_obtain_name_two = m_obtain_name_one.replace("</UNAME>","");
			ajax_list_activeInput.value = m_obtain_name_two;
		}else{
			ajax_list_activeInput.value = tmpValue;
		}
		if(document.getElementById(ajax_list_activeInput.name + '_hidden'))document.getElementById(ajax_list_activeInput.name + '_hidden').value = inputObj.id; 
		ajax_options_hide();
		if(sel_option == "UNIVERSITY"){
		var obtain_id = inputObj.id;
		p_obtain_id=obtain_id.replace(/<uId>/i,"");
		popdegreeList(p_obtain_id);			
		}
	}
	
	function ajax_options_hide()
	{
		if(ajax_optionDiv)ajax_optionDiv.style.display='none';	
		if(ajax_optionDiv_iframe)ajax_optionDiv_iframe.style.display='none';
	}

	function ajax_options_rollOverActiveItem(item,fromKeyBoard)
	{
		if(ajax_list_activeItem)ajax_list_activeItem.className='optionDiv';
		item.className='optionDivSelected';
		ajax_list_activeItem = item;
		
		if(fromKeyBoard){
			if(ajax_list_activeItem.offsetTop>ajax_optionDiv.offsetHeight){
				ajax_optionDiv.scrollTop = ajax_list_activeItem.offsetTop - ajax_optionDiv.offsetHeight + ajax_list_activeItem.offsetHeight + 2 ;
			}
			if(ajax_list_activeItem.offsetTop<ajax_optionDiv.scrollTop)
			{
				ajax_optionDiv.scrollTop = 0;	
			}
		}
	}
	
	function ajax_option_list_buildList(letters,paramToExternalFile)
	{
		
		ajax_optionDiv.innerHTML = '';
		ajax_list_activeItem = false;
		if(ajax_list_cachedLists[paramToExternalFile][letters.toLowerCase()].length<=1){
			ajax_options_hide();
			return;			
		}
		
		
		
		ajax_list_optionDivFirstItem = false;
		var optionsAdded = false;
		for(var no=0;no<ajax_list_cachedLists[paramToExternalFile][letters.toLowerCase()].length;no++){
			if(ajax_list_cachedLists[paramToExternalFile][letters.toLowerCase()][no].length==0)continue;
			optionsAdded = true;
			var div = document.createElement('DIV');
			var items = ajax_list_cachedLists[paramToExternalFile][letters.toLowerCase()][no].split('</uId>');
			if(ajax_list_cachedLists[paramToExternalFile][letters.toLowerCase()].length==1 && ajax_list_activeInput.value == items[0]){
				ajax_options_hide();
				return;						
			}
			
			
			div.innerHTML = items[items.length-1];
			div.id = items[0];
			div.className='optionDiv';
			div.onmouseover = function(){ ajax_options_rollOverActiveItem(this,false) }
			div.onclick = ajax_option_setValue;
			if(!ajax_list_optionDivFirstItem)ajax_list_optionDivFirstItem = div;
			ajax_optionDiv.appendChild(div);
		}	
		if(optionsAdded){
			ajax_optionDiv.style.display='block';
			if(ajax_optionDiv_iframe)ajax_optionDiv_iframe.style.display='';
			ajax_options_rollOverActiveItem(ajax_list_optionDivFirstItem,true);
		}
					
	}
	
	function ajax_option_list_showContent(ajaxIndex,inputObj,paramToExternalFile,whichIndex)
	{
		if(whichIndex!=currentListIndex)return;
		var letters = inputObj.value;
		var content = ajax_list_objects[ajaxIndex].response;
		var elements = content.split('<University>');
		ajax_list_cachedLists[paramToExternalFile][letters.toLowerCase()] = elements;
		ajax_option_list_buildList(letters,paramToExternalFile);		
	}
	
	function ajax_option_resize(inputObj)
	{
		ajax_optionDiv.style.top = (ajax_getTopPos(inputObj) + inputObj.offsetHeight + ajaxBox_offsetY) + 'px';
		ajax_optionDiv.style.left = (ajax_getLeftPos(inputObj) + ajaxBox_offsetX) + 'px';
		if(ajax_optionDiv_iframe){
			ajax_optionDiv_iframe.style.left = ajax_optionDiv.style.left;
			ajax_optionDiv_iframe.style.top = ajax_optionDiv.style.top;			
		}		
		
	}
	
	function ajax_showOptions(inputObj,paramToExternalFile,e)
	{	
		
		var country = document.getElementById("country").value;	
	     
		var universityName=document.getElementById("universityName").value;
		var url="";
		if(universityName == "null" || universityName == "" || universityName =="a specific UNIVERSITY")
		{
			paramToExternalFile = "autoCompleteDisciplineSearch";			
		}
		if(e.keyCode==13 || e.keyCode==9)return;
		if(ajax_list_currentLetters[inputObj.name]==inputObj.value)return;
		if(!ajax_list_cachedLists[paramToExternalFile])ajax_list_cachedLists[paramToExternalFile] = new Array();
		ajax_list_currentLetters[inputObj.name] = inputObj.value;
		if(!ajax_optionDiv){
			ajax_optionDiv = document.createElement('DIV');
			ajax_optionDiv.id = 'ajax_listOfOptions';	
			document.body.appendChild(ajax_optionDiv);
		
			if(ajax_list_MSIE){
				ajax_optionDiv_iframe = document.createElement('IFRAME');
				ajax_optionDiv_iframe.border='0';
				ajax_optionDiv_iframe.style.width = ajax_optionDiv.clientWidth + 'px';
				ajax_optionDiv_iframe.style.height = ajax_optionDiv.clientHeight + 'px';
				ajax_optionDiv_iframe.id = 'ajax_listOfOptions_iframe';
				
				document.body.appendChild(ajax_optionDiv_iframe);
			}
			
			var allInputs = document.getElementsByTagName('INPUT');
			for(var no=0;no<allInputs.length;no++){
				if(!allInputs[no].onkeyup)allInputs[no].onfocus = ajax_options_hide;
			}			
			var allSelects = document.getElementsByTagName('SELECT');
			for(var no=0;no<allSelects.length;no++){
				allSelects[no].onfocus = ajax_options_hide;
			}

			var oldonkeydown=document.body.onkeydown;
			if(typeof oldonkeydown!='function'){
				document.body.onkeydown=ajax_option_keyNavigation;
			}else{
				document.body.onkeydown=function(){
					oldonkeydown();
				ajax_option_keyNavigation() ;}
			}
			var oldonresize=document.body.onresize;
			if(typeof oldonresize!='function'){
				document.body.onresize=function() {ajax_option_resize(inputObj); };
			}else{
				document.body.onresize=function(){oldonresize();
				ajax_option_resize(inputObj) ;}
			}
				
		}
		
		if(inputObj.value.length<minimumLettersBeforeLookup){
			ajax_options_hide();
			return;
		}
				

		ajax_optionDiv.style.top = (ajax_getTopPos(inputObj) + inputObj.offsetHeight + ajaxBox_offsetY) + 'px';
		ajax_optionDiv.style.left = (ajax_getLeftPos(inputObj) + ajaxBox_offsetX) + 'px';
		if(ajax_optionDiv_iframe){
			ajax_optionDiv_iframe.style.left = ajax_optionDiv.style.left;
			ajax_optionDiv_iframe.style.top = ajax_optionDiv.style.top;			
		}
		
		ajax_list_activeInput = inputObj;
		ajax_optionDiv.onselectstart =  ajax_list_cancelEvent;
		currentListIndex++;
		if(ajax_list_cachedLists[paramToExternalFile][inputObj.value.toLowerCase()]){
			ajax_list_cachedLists[paramToExternalFile] = new Array();
			ajax_list_currentLetters[inputObj.name] = inputObj.value;
			
			var tmpIndex=currentListIndex/1;
			ajax_optionDiv.innerHTML = '';
			var ajaxIndex = ajax_list_objects.length;
			ajax_list_objects[ajaxIndex] = new sack();
			if(paramToExternalFile == "autoCompleteUniversitySearch"){
			sel_option = "UNIVERSITY";			
			document.getElementById("qualificationid").value = "-1";
			document.getElementById("course").value = "-1";
			
			//extra code added for dis populating course and disciplines based on university on oct24
			document.getElementById("qualificationid").options.length= 1;
			document.getElementById("course").options.length= 1;
			//end of dis populating course and disciplines based on university on oct24
			
			document.form1.discipline.value="";
			//document.form1.hiddisciplines.innerHTML="";
			url = ajax_list_externalFile + '?flag=' + paramToExternalFile + '&id=' + inputObj.value.replace(" ","+") + '&country=' + country;
			}else if(paramToExternalFile == "autoCompleteSubjectSearch"){
			sel_option = "SUBJECT";
			url = ajax_list_externalFile + '?flag=' + paramToExternalFile + '&id=' + inputObj.value.replace(" ","+") + '&universityName=' + universityName;	
			}else if(paramToExternalFile == "autoCompleteDisciplineSearch"){
			sel_option = "DISCIPLINE";
			url = ajax_list_externalFile + '?flag=' + paramToExternalFile + '&id=' + inputObj.value.replace(" ","+") + '&country=' + country;	
			}

			ajax_list_objects[ajaxIndex].requestFile = url;	// Specifying which file to get
			ajax_list_objects[ajaxIndex].onCompletion = function(){ ajax_option_list_showContent(ajaxIndex,inputObj,paramToExternalFile,tmpIndex); };		
			// Specify function that will be executed after file has been found
			ajax_list_objects[ajaxIndex].runAJAX();		// Execute AJAX function	
		}else{
			var tmpIndex=currentListIndex/1;
			ajax_optionDiv.innerHTML = '';
			var ajaxIndex = ajax_list_objects.length;
			ajax_list_objects[ajaxIndex] = new sack();
			if(paramToExternalFile == "autoCompleteUniversitySearch"){
			sel_option = "UNIVERSITY";			
			document.getElementById("qualificationid").value = "-1";
			document.getElementById("course").value = "-1";
			document.form1.discipline.value="";
			//document.form1.hiddisciplines.innerHTML="";
			url = ajax_list_externalFile + '?flag=' + paramToExternalFile + '&id=' + inputObj.value.replace(" ","+") + '&country=' + country;
			}else if(paramToExternalFile == "autoCompleteSubjectSearch"){
			sel_option = "SUBJECT";
			url = ajax_list_externalFile + '?flag=' + paramToExternalFile + '&id=' + inputObj.value.replace(" ","+") + '&universityName=' + universityName;	
			}else if(paramToExternalFile == "autoCompleteDisciplineSearch"){
			sel_option = "DISCIPLINE";
			url = ajax_list_externalFile + '?flag=' + paramToExternalFile + '&id=' + inputObj.value.replace(" ","+") + '&country=' + country;	
			}

			ajax_list_objects[ajaxIndex].requestFile = url;	// Specifying which file to get
			ajax_list_objects[ajaxIndex].onCompletion = function(){ ajax_option_list_showContent(ajaxIndex,inputObj,paramToExternalFile,tmpIndex); };		
			// Specify function that will be executed after file has been found
			ajax_list_objects[ajaxIndex].runAJAX();		// Execute AJAX function				
		}
		
			
	}
	
	function ajax_option_keyNavigation(e)
	{
		if(browser_find != "macsafari"){
		if(document.all)e = event;
		
		if(!ajax_optionDiv)return;
		if(ajax_optionDiv.style.display=='none')return;
		
		if(e.keyCode==38){	// Up arrow
			if(!ajax_list_activeItem)return;
			if(ajax_list_activeItem && !ajax_list_activeItem.previousSibling)return;
			ajax_options_rollOverActiveItem(ajax_list_activeItem.previousSibling,true);
		}
		
		if(e.keyCode==40){	// Down arrow
			if(!ajax_list_activeItem){
				ajax_options_rollOverActiveItem(ajax_list_optionDivFirstItem,true);
			}else{
				if(!ajax_list_activeItem.nextSibling)return;
				ajax_options_rollOverActiveItem(ajax_list_activeItem.nextSibling,true);
			}
		}
		
		if(e.keyCode==13 || e.keyCode==9){	// Enter key or tab key
			if(ajax_list_activeItem && ajax_list_activeItem.className=='optionDivSelected')ajax_option_setValue(false,ajax_list_activeItem);
			if(e.keyCode==13)return false; else return true;
		}
		if(e.keyCode==27){	// Escape key
			ajax_options_hide();			
		}
		}
	}
	
	
	document.documentElement.onclick = autoHideList;
	
	function autoHideList(e)
	{
		if(document.all)e = event;
		
		if (e.target) source = e.target;
			else if (e.srcElement) source = e.srcElement;
			if (source.nodeType == 3) // defeat Safari bug
				source = source.parentNode;		
		if(source.tagName.toLowerCase()!='input' && source.tagName.toLowerCase()!='textarea')ajax_options_hide();
		
	}
</script>
<script type="text/javascript">
/* Populating DegreeList --------------------------------------------------*/
function popdegreeList(universityID) {
	var given_universityID = universityID ;
	if ((given_universityID != ""))
	{
		var degreeid = 'qualificationid';
		document.getElementById("course").value = "-1";
		document.form1.discipline.value="";
		//document.form1.hiddisciplines.innerHTML="";
		url="retriveCourses.htm?universityID="+universityID+"&flag=changeUniverity"; ; 
		ajaxcaldegrees(url,degreeid); 
	} 
 
}


function ajaxcaldegrees(url,degreeid)
{
	var page_request = false; 
	if (window.XMLHttpRequest) 
       page_request = new XMLHttpRequest();
	else if (window.ActiveXObject){ 
		try {page_request = new ActiveXObject("Msxml2.XMLHTTP");}catch (e)
		{try{page_request = new ActiveXObject("Microsoft.XMLHTTP");}catch (e){}}}
		else{
		   document.getElementById(degreeid).innerHTML="Browser Does Not Support This Application";
		   return ;
		}
	page_request.onreadystatechange=function(){
	if(page_request.readyState == 4)
	{     
     	  if(page_request.status==200)
		  {
				document.form1.qualificationid.length=0; 
				var  List2 = new Array();
				var idList = new Array();
			    var nameList = new Array();				
			    var List1=page_request.responseText;
				if(List1.length==0){
				     document.form1.qualificationid.length=0; 
				     document.form1.qualificationid.options.add(new Option('Select One', '-1'));
				     document.form1.course.length=0;
					 document.form1.course.options.add(new Option('Select One', '-1'));
				     document.form1.discipline.value=List1;		
				     document.getElementById("hiddisciplines").innerHTML=List1;				 
				}	
				else{
					 List2=List1.split("##");
				     idList = List2[0].split("±");
					 nameList = List2[1].split("±"); 
					 document.form1.qualificationid.options[document.form1.qualificationid.options.length]=(new Option('Select One', '-1'));
			         for(var i = 0; i < idList.length; i++)
					 {			  
			         document.form1.qualificationid.options[document.form1.qualificationid.options.length]= (new Option(nameList[i],idList[i]));
					 }		  
	      }
	      }
        }
	}
	page_request.open('GET', url, true);
	page_request.send(null);
  }
//Ajax code ends here*******************************************************************************************


//Courses Population*************************************************************************************************************

function popCourses(Qid)  {
	var given_Qid = Qid ;
	if ((given_Qid != ""))
	{
		
		if(p_obtain_id==0)
		{
		        p_obtain_id=document.form1.hiddenUnivID.value;
		}
		var cid = 'course';		
		document.form1.discipline.value="";
		//document.form1.hiddisciplines.innerHTML="";
		url="retriveCourses.htm?quaId="+Qid+"&uniID="+p_obtain_id+"&flag=changeQalif"; 
		ajaxcal(url,cid);  
	}	 
}


function ajaxcal(url,cid)
{ 
	var page_request = false; 
	if (window.XMLHttpRequest) 
		page_request = new XMLHttpRequest();
	else if (window.ActiveXObject){ 
		try {page_request = new ActiveXObject("Msxml2.XMLHTTP");}catch (e)
		{try{page_request = new ActiveXObject("Microsoft.XMLHTTP");}catch (e){}}}
			else{
			   document.getElementById(cid).innerHTML="Browser Does Not Support This Application";
			   return ;
	}
	page_request.onreadystatechange=function(){
	if(page_request.readyState == 4)
	{     
   	  if(page_request.status==200)
	  {
			document.form1.course.length=0; 
			var  List2 = new Array();
		    var idList = new Array();
	        var nameList = new Array();
	        var List1=page_request.responseText;
		    if(List1.length==0){
			      document.form1.course.length=0;
			      document.form1.course.options.add(new Option('Select One', '-1'));
				  document.form1.discipline.value=List1;
				  document.getElementById("hiddisciplines").innerHTML=List1;
			}else{
				  List2=List1.split("##");
			      idList = List2[0].split("±");
				  nameList = List2[1].split("±"); 
		          document.form1.course.options[document.form1.course.options.length]=(new Option('Select One', '-1'));	
			      for(var i = 0; i < idList.length; i++)
				  {			  
			           	document.form1.course.options[document.form1.course.options.length]=(new Option(nameList[i],idList[i]));
				  }	  
	        }	   
	   }
	   }
	}	
		page_request.open('GET', url, true);
		page_request.send(null);
	}
//Courses Population End-------------------------------
//Ajax code  for populating Disciplines*************************************************************************
 
 function popDisciplines(CourseId)  { 
		var given_CourseId = CourseId ;
		change="change";
		if ((given_CourseId != ""))
        {
			 var did = 'discipline';
			 url="retriveCourses.htm?CourseId="+CourseId+"&flag=changeCourse"; 
			 showDisciplines(url,did);           
		}  
}

 function showDisciplines(url,did)
 {
	 var page_request = false;
     if (window.XMLHttpRequest) 
		 page_request = new XMLHttpRequest();
	     else if (window.ActiveXObject){ 
		 try {page_request = new ActiveXObject("Msxml2.XMLHTTP");}catch (e)
		 {try{page_request = new ActiveXObject("Microsoft.XMLHTTP");}catch (e){}}}
		 else{
			 document.getElementById(cid).innerHTML="Browser Does Not Support This Application";
			 return ;
		 }
		 page_request.onreadystatechange=function(){
		 if(page_request.readyState == 4)
		 {
			document.form1.discipline.value="";   
			if(page_request.status==200)
			{		 
				var disc=page_request.responseText;
		 	    document.form1.discipline.value = disc;
	 		    document.getElementById("hiddisciplines").innerHTML = disc;
	        }
	        else{
				document.form1.discipline.value = disc;
				document.getElementById("hiddisciplines").innerHTML = disc;
			}
		}
	}
	page_request.open('GET', url, true);
	page_request.send(null);
  } 
//End Of Ajax ************************************************************************************************
function hiddenPopulate()
{
	var i=document.form1.discipline.value;
	var courseId=document.form1.course.value;
<%if(request.getSession().getAttribute("disciplineFlag")==null){%>
		popDisciplines(courseId);
	<%}
	  else if(!((request.getSession().getAttribute("disciplineFlag")).toString()).equals("update")){%>
		  popDisciplines(courseId);
	<%}
	%>
  document.getElementById("hiddisciplines").innerHTML=i;
	<%request.getSession().removeAttribute("disciplineFlag");%>
}

</script>


<script type="text/javascript">
var change="noChange";
function countryChange()
{
document.form1.universityName.value="";
document.getElementById("course").value = "-1";
document.getElementById("qualificationid").value = "-1";
document.form1.discipline.value="";
//document.form1.hiddisciplines.innerHTML="";
}
	function enableValue(){
			document.form1.discipline.disabled="true";
		}		

		function disable()
		{
			var univemail= document.getElementById("univemail").value;
				if(univemail.length<=0)
				{
					document.form1.univemail.enabled="true"
				}else{
					document.form1.univemail.disabled="true";
				}
		}

 
		function editDisciplines(){
			document.form1.univemail.disabled=null;  
			var url = "editDisciplinesProcess.htm?formView=registraton&change="+change;
			document.form1.action=url;
			document.form1.submit();
		}
 
		function home()
		{
			location.href="hello.htm?id1=logout";
		}
 <%@ include file="/js/headerFunctions.js" %>
</script>
	</head>
	<%
				String activationSession = (String) SessionManager.getSessionAttribute(SessionAttributeKey.activationId,request);
		if (activationSession == null) {
	%>
	<body onload="document.form1.univemail.disabled=false;document.form1.oldPassword.focus();javascript:hiddenPopulate();">
		<%
			}

			else {
		%>
	
	<body oncontextmenu="return false" onload="javascript:hiddenPopulate();">

		<%
		}
		%>
		
<form:form name="form1" commandName="UserDTO" >
	
<div id="wrapper">
 <div id="nav_top">
		<ul>
		  <li><a href="javascript:home();"><fmt:message key="registration.lable.home"/></a></li>
		  <li><img src="images/navtop_divider.gif" /></li>
		  <li><a href="Searchnlu.htm?id=nonLoggedInUser"><fmt:message key="registration.lable.search"/></a></li>
		  <li><img src="images/navtop_divider.gif" /></li>
		  <li><a href="#"><fmt:message key="registration.lable.rate"/></a></li>
		  <li><img src="images/navtop_divider.gif" /></li>
		  <li><a href="inProcessLoginnlu.htm?id=editprofile" class="active"><fmt:message key="registration.lable.myprofile"/></a></li>
		  <li><img src="images/navtop_divider.gif" /></li>
		  <li><a href="inProcessLoginnlu.htm?id=folder"><fmt:message key="registration.lable.myfolder"/></a></li>
		  <li><img src="images/navtop_divider.gif" /></li>
		  <li><a href="inProcessLoginnlu.htm?id=contribution"><fmt:message key="registration.lable.mycontributions"/></a></li>
		  <li><img src="images/navtop_divider.gif" /></li>
		  <li><a href="#"><fmt:message key="registration.lable.myrewards"/></a></li>
		  <li><img src="images/navtop_divider.gif" /></li>
			<li><a href="inProcessLoginnlu.htm?id=messages"><fmt:message key="registration.lable.mymessages"/></a></li>
	  	</ul>
	</div>
	<%@ page import= "com.mgh.sps.common.util.SessionManager,com.mgh.sps.common.util.SessionAttributeKey" %>
				<div id="headerCrumb">
					<div class="floatL">
		 				<img src="images/logo_en_uk.gif" title="GradeGuru, note sharing by students for students"/>
					</div>
					<div class="crumb">
					<div class="crumbLink">  &nbsp;
					<!-- <a href="#"><fmt:message key="registration.lable.home"/></a> > <fmt:message key="registration.lable.myprofile"/> --> </div>
					
             <div class="welcomeName">
				<fmt:message key="registration.lable.welcome"/> <span class="guruName"><%=(String)SessionManager.getSessionAttribute(SessionAttributeKey.tempName, request)%></span>&nbsp;:&nbsp;
				<a href="javascript:home()" class="logout_global"><fmt:message key="registration.lable.logout"/></a>
			 </div>
			 </div>
				</div>
				<div style="clear:both"></div>

				<div id="body">

					<%
					if (AppConstants.flag3.equals("y")) {
					%>
					<div class="blockHeader">
						Alert...
					</div>
					<div class="blockalert">
						<table>
							<tr>
								<td width="42">
									<img src="images/alert.gif" width="36" height="35" align="left" />
								</td>
								<td colspan="2">
									Please correct the entries below. We either had trouble
									understanding those fields,
									<br />
									or need more information:
								</td>
							</tr>
							<tr>
								<td>&nbsp;
									
								</td>
								<td width="252">
									<ul>
										<form:errors path="*" />


									</ul>
								</td>
							</tr>
						</table>
					</div>
					<%
					}
					%>




					<div class="blockHeader">
						<fmt:message key="registration.lable.Myprofile"/>
					</div>
					 <form:hidden path="username"/>
					  <form:hidden path="aboutUser"/>
					<div>
						<div class="mBlock">
							<div class="PformSection">
								<div class="formTxt">
									<fmt:message key="registration.lable.oldpassword"/>
								</div>
								<div class="PformField">
									<form:password path="oldPassword"  maxlength="50" size="10"
										showPassword="true" tabindex="1"/>
								</div>
							</div>

							<div class="PformSection">
								<div class="formTxt">
									<fmt:message key="registration.lable.newpassword"/>
								</div>
								<div class="PformField">
									<form:password path="newPassword" maxlength="50" size="10"
										showPassword="true" tabindex="2"/>
									<br />
									(Up to&nbsp;8 characters)
								</div>
							</div>



							<div class="PformSection">
								<div class="formTxt">
									<fmt:message key="registration.lable.cnfrmnew_password"/>
								</div>
								<div class="PformField">
									<form:password path="confirmPassword" maxlength="50" size="10"
										showPassword="true" tabindex="3"/>
								</div>
								
							</div>
							
							<div class="PformSection">
								<div class="formTxt">
									<fmt:message key="registration.lable.securityquestion"/>
								</div>
								<div class="PformField">
									<form:select path="questionId" tabindex="4">
										<form:option value="-1">Select One</form:option>
										<form:options items="${secQuestions}" itemLabel="value"
											itemValue="key" />

									</form:select>
								</div>
							</div>
							<div class="PformSection">
								<div class="formTxt">
									<fmt:message key="registration.lable.securityanswer"/>
								</div>
								<div class="PformField">
									<form:input path="answer" maxlength="50" size="50" tabindex="5"/>
								</div>
							</div>
                       
    
		
		 <%String status1=(String)SessionManager.getSessionAttribute(SessionAttributeKey.loginStatus,request);%>
		  <%if(status1!=null){%>
							<div class="PformSection">
								<div class="formTxt">
									<fmt:message key="registration.lable.universityemail"/>
								</div>
								<div class="PformField">
								<% if(status1.equals("universityEmail"))
								{ %>
									<form:input path="hiddenunivemail" maxlength="255" size="80"  title="email provided by your university" onkeydown="if(event.keyCode==9){return true}else{return false}" tabindex="6" disabled="true"/>
									<form:hidden path="univemail"/>
									<% }else { %>
									<form:input path="univemail"  title="email provided by your university" maxlength="255" size="80" tabindex="6"/>  <% }} %>
								 
								</div>
							</div>
							<div class="PformSection">
								<div class="formTxt">
									<fmt:message key="registration.lable.preferedemail"/>
								</div>
								<div class="PformField">
									<form:input path="preferemail" maxlength="255" size="80" title="email address you most commonly use" tabindex="7"/>
								</div>
							</div>
							<div class="PformSection">
								<div class="formTxt">
									<fmt:message key="registration.lable.yearofstudy"/>
								</div>
								<div class="PformField">
									<form:select path="currentYear1" title="what year are you in or going into?" tabindex="8">
										<form:option value="-1">-- Current Year --</form:option>
										<form:options items="${yearLevelStudy}" itemLabel="value"
											itemValue="key" />
									</form:select>
								</div>
							</div>
						</div>


						<div class="srBlock">
							<div class="blockBlue">
								<strong>Hi <%=SessionManager.getSessionAttribute(SessionAttributeKey.tempName, request)%>.</strong>
								Glad to have you!
								<p>
									Please fill in the details to the left so GradeGuru can make
									your site experience a positive one.
								</p>
								<p>
									* mandatory field
								</p>
								 <br>
							</div>
						</div>
					</div>
					<div style="clear:both"></div>

					<div class="FformSection2">
						<div class="formTxt2">
							<fmt:message key="registration.lable.universitycountry"/>
						</div>
						<div class="FformField2">
							<form:select path="universitycountry" title="primary university location you do or did attend" id="country"   onchange="javascript:countryChange();" tabindex="9">
								<form:option value="-1">Select One</form:option>
								<form:options items="${countries}" itemLabel="value"
									itemValue="key" />
							</form:select>
						</div>
					</div>

			<div class="FformSection2" >
				<div class="formTxt2"><fmt:message key="registration.lable.universityname"/></div>
				<div class="FformField2"><form:input  path="universityName" title="name of the primary institution you do or did attend" onkeyup="ajax_showOptions(this,'autoCompleteUniversitySearch',event);" tabindex="10"/>				
				</div>			
			</div>
              <form:hidden path="hiddenUnivID"/>

					<div class="FformSection2">
						<div class="formTxt2">
							<fmt:message key="registration.lable.qualification"/>
						</div>
						<div class="FformField2">
							<form:select path="qualificationid" title="qualification you are studying for or graduated with"  onchange="javascript:popCourses(this.options[selectedIndex].value);" tabindex="11">
								<form:option value="-1"  >Select One</form:option>
								<form:options items="${degreeList}" itemLabel="value"  
									itemValue="key" />

							</form:select>
						</div>
					</div>
					<div class="FformSection2">
						<div class="formTxt2">
							<fmt:message key="registration.lable.course"/>
						</div>
						<div class="FformField2" id="courseID">
							<form:select path="course" title="course you are enrolled in or recently graduated from" onchange="javascript:popDisciplines(this.options[selectedIndex].value);" tabindex="12">
								<form:option value="-1">Select One</form:option>
								<form:options items="${courselist}" itemLabel="value"
									itemValue="key" />

							</form:select>
						</div>
					</div>

					<div class="FformSection2">
						<div class="formTxt2">
							<fmt:message key="registration.lable.course_disciplines"/>
						</div>
						<div class="FformField2">
							<div id="hiddisciplines" title="the subject areas covered in the core of your course" style="padding: 0px 0px 10px 0px; overflow: auto; float:left; font-style:normal;font-size:12px; color:#999999; border: 1px solid #7f9db9; width: 505px; height: 48px;"/>
							&nbsp;</div>
							<div style="margin-top:46px">&nbsp;
							<a href="javascript:editDisciplines();" tabindex="14"><fmt:message key="registration.lable.editdisciplines"/></a>
						   <form:hidden path="discipline"/>
				 				 </div>
						</div>
					</div>
					<div class="FformSection2">
						<div class="formTxt2">
							<fmt:message key="registration.lable.fullorpart"/>
						</div>
						<div class="FformField2">
							<form:select path="fulOrPart" tabindex="15" title="the format of your studies and attendance">
								<form:option value="-1">Select One</form:option>
								<form:options items="${studyMode}" itemLabel="value"
									itemValue="key" />

							</form:select>
						</div>
					</div>
					<div class="FformSection">
						<div align="right">
							<input name="" type="submit" value="Submit" class="btn_login" tabindex="16"/>
						</div>
					</div>
				</div><%@ include file="/jsp/footer.jsp" %>
			</div>

			
</div>

		</form:form>


	</body>
</html>
