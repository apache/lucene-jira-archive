<%
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
response.setDateHeader("Expires", 0); //prevents caching at the proxy server
response.setHeader("Cache-Control", "private"); // HTTP 1.1 
response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 
response.setHeader("Cache-Control", "max-stale=0"); // HTTP 1.1 
%><%/* --------------------------------------------------------------------------------------------
 Hurix Systems Pvt. Ltd.

 Version Information :
 
 Version Number		    : 1.00
 Coded by				: Muarali Krishna.S
 Release Date			: 
 
 -------------------------------- Description ---------------------------------------------------
This jsp is used to display search page. 
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
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<link href="css/styles.css" rel="stylesheet" type="text/css" />
<!-- CODE ADDED FOR STUBBING --->
<script type="text/javascript" src="js/addEvent.js"></script>
<script type="text/javascript" src="js/sweetTitles.js"></script>
<!-- CODE ENDED FOR STUBBING --->

<style type="text/css">
	/* Big box with list of options */
	#ajax_listOfOptions{
		position:absolute;	/* Never change this one */
		width:375px;	/* Width of box */
		height:150px;	/* Height of box */
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
	document.write('<style type="text/css"> #ajax_listOfOptions{ margin-top:10px; margin-left:137px; } </style>');
}
if(browser_find == "mac_mozilla"){	
	document.write('<style type="text/css"> #ajax_listOfOptions{ margin-top:10px; margin-left:117px; } </style>');
}
if(browser_find == "macsafari" && mac_ver == "PPC"){	
	document.write('<style type="text/css"> #ajax_listOfOptions{ margin-top:297px; margin-left:198px; } </style>');
}
if(browser_find == "macsafari" && mac_ver == "Intel"){	
	document.write('<style type="text/css"> #ajax_listOfOptions{ margin-top:297px; margin-left:212px; } </style>');
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
	var ajax_list_externalFile = 'retriveCoursesnlu.htm';	// Path to external file
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
		var p_obtain_id=obtain_id.replace(/<uId>/i,"");
		//popQualificationList(p_obtain_id);
		}//alert(p_obtain_id);	
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
		 
		
		if(paramToExternalFile=="autoCompleteUniversitySearch")
		{
			paramToExternalFile="autoCompleteUniversitySearchNoCountry";
		} else if(paramToExternalFile=="autoCompleteSubjectSearch")
		{
			paramToExternalFile="autoCompleteDisciplineSearchNoCountry";
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
			ajax_option_list_buildList(inputObj.value,paramToExternalFile,currentListIndex);			
		}else{
			var tmpIndex=currentListIndex/1;
			ajax_optionDiv.innerHTML = '';
			var ajaxIndex = ajax_list_objects.length;
			ajax_list_objects[ajaxIndex] = new sack();
			if(paramToExternalFile == "autoCompleteUniversitySearchNoCountry") {
				sel_option = "UNIVERSITY";
				url = ajax_list_externalFile + '?flag=' + paramToExternalFile + '&id=' + inputObj.value.replace(" ","+")+ '&countryId=' +country ;	
			}
			
			else if(paramToExternalFile == "autoCompleteDisciplineSearchNoCountry") {
			sel_option = "SUBJECT";
			url = ajax_list_externalFile + '?flag=' + paramToExternalFile + '&id=' + inputObj.value.replace(" ","+") ;	
			} 	

		 
			var changeObj=new changedObject(inputObj+"countryId="+country)
		 
			ajax_list_objects[ajaxIndex].requestFile = url;	// Specifying which file to get
			ajax_list_objects[ajaxIndex].onCompletion = function(){
			ajax_option_list_showContent(ajaxIndex,changeObj,paramToExternalFile,tmpIndex); };	// Specify function that will be executed after file has been found
			ajax_list_objects[ajaxIndex].runAJAX();		// Execute AJAX function				
		}
		
			
	}
	
function changedObject(value)
{
	this.value=value
}

	function ajax_option_keyNavigation(e)
	{	if(browser_find != "macsafari"){
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
//Start popdegreeList *****************************************
function popQualificationList(universityId) {
	var given_universityId = universityId ;
	if ((given_universityId != ""))
	{
		var degreeid = 'qualification';
		url="retriveCoursesnlu.htm?universityId="+universityId+"&flag=changeSubjectSearch"; 
		ajaxcaldegrees(url,degreeid); 
	}
}

function ajaxcaldegrees(url,degreeid){
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
			document.form1.qualification.length=0; 
			var  List2 = new Array();
			var idList = new Array();
			var nameList = new Array();
			var List1=page_request.responseText;
			if(List1.length==0){
				document.form1.qualification.length=0; 
				document.form1.qualification.options.add(new Option('All Qualification Types', 'select'));
				document.form1.yearLevel.length=0;
				document.form1.yearLevel.options.add(new Option('All Year Levels', 'select'));
				document.form1.specificType.length=0;
				document.form1.specificType.options.add(new Option('All Types', 'select'));
			 }else{
				List2=List1.split("##");
			    idList = List2[0].split("±");
				nameList = List2[1].split("±");		
				document.form1.qualification.options[document.form1.qualification.options.length]=(new Option('All Qualification Types', 'select'));
				for(var i = 0; i < idList.length; i++)
				{
           		document.form1.qualification.options[document.form1.qualification.options.length]=(new Option(nameList[i],idList[i]));
				}		    
		}
	}
  }
}
  page_request.open('GET', url, true);
  page_request.send(null);
}
//End popdegreeList *****************************************
</script>

<script type="text/javascript">
function countryChange()
{
document.form1.universityName.value="a specific UNIVERSITY";
document.getElementById("subjectArea").value = "a specific SUBJECT AREA";
document.getElementById("qualification").value = "select";
}
function KeyWords()
{	
	if((document.getElementById("keywords").value)== "Keyword Search")
	{
	document.getElementById("keywords").value="";
	}
}
function UniversityName()
{
	if((document.getElementById("universityName").value)== "a specific UNIVERSITY")
	{
	document.getElementById("universityName").value="";	
	}
}
function SubjectArea()
{
	if((document.getElementById("subjectArea").value)== "a specific SUBJECT AREA")
	{
	document.getElementById("subjectArea").value="";
	}
}
function inprocessLogin()
{
	location.href="inProcessLoginnlu.htm?id=searchfinal";
}
function home()
{
	location="login.action";
}
function searchTips()
{
	location.href="hello.htm?id1=searchTips";
}
function SearchResults()
{
	location.href="SearchResultsnlu.htm";
}
function setDefaultUniversity()
{
 
document.form1.universityName.value="a specific UNIVERSITY";
}
<%@ include file="/js/headerFunctions.js" %>
</script>

<title>GradeGuru, note sharing by students for students</title>

</head>

<body>


<form:form name="form1" commandName="command">
		<div id="wrapper">
<%@ include file="/jsp/header.jsp" %>
   <%@ page import= "com.mgh.sps.common.util.SessionManager,com.mgh.sps.common.util.SessionAttributeKey" %>
	<div id="headerCrumb">
	  	<div class="floatL"><img src="images/logo_en_uk.gif" title="GradeGuru, note sharing by students for students" /></div>
		<div class="crumb"> 
		<%if(id==null){%><div class="crumbLink"> &nbsp;
		<!-- <a href="javascript:logout()"><fmt:message key="search.lable.home"/></a> > <fmt:message key="search.lable.search"/> --> </div>
		<%}else {%>
			<div class="crumbLink"> &nbsp;
			<!-- <a href="javascript:loggedHome()"><fmt:message key="search.lable.home"/></a> > <fmt:message key="search.lable.search"/> --> </div>
			<%} %>
			<%if(id==null){
		%>	<div class="welcomeName">
		<a href="javascript:inprocessLogin()" class="logout_global"><fmt:message key="search.lable.login"/></a>
		</div><%}else {%>
			<div class="welcomeName"> 
				<fmt:message key="search.lable.welcome"/>&nbsp;<span class="guruName"><%=(String)SessionManager.getSessionAttribute(SessionAttributeKey.tempName, request)%></span> :&nbsp; 
				<a href="javascript:logout()" class="logout_global"><fmt:message key="search.lable.logout"/></a>
			</div>
			<%}%>
		</div>
	</div>
	
	<div style="clear:both"></div>
	
	<div id="body">
		<div class="blockHeader">
		<div class="floatL"><fmt:message key="search.lable.Search"/></div>
		<!--    COMMENTED FOR STUBBING -->  
		<!-- <div class="floatR"><a href="javascript:searchTips()"><fmt:message key="search.lable.searchtips"/></a></div>
		
--> <!-- END OF STUBBING -->
<div class="floatR"><a href="javascript:dummy()" title="Coming soon: refer to the GradeGuru Site Updates on your home page"><fmt:message key="search.lable.searchtips"/></a> </div>
		</div>
<div class="blockalert"><form:errors path="*" /></div>
		<div id="searchpaper">
			<div class="header6">
				Dear GradeGuru, 
				<div style="clear:both"><img src="images/spacer.gif" height="7px" /></div>
				I want to search for notes covering the following topics *
		  <div style="clear:both"><img src="images/spacer.gif" height="7px" /></div>
		  </div>			 		 
				<div class="section2">
				<div class="left">&nbsp;</div>
				<div class="fields">
				<form:input path="keywords" maxlength="80" tabindex="1" onfocus="KeyWords();" />
					</div>
				</div> 
				<div style="clear:both"><img src="images/spacer.gif" height="5px" /></div>
			
			 <div class="header7">
				and I only want notes contributed by students?
			 </div>
			 <div style="clear:both"><img src="images/spacer.gif" height="5px" /></div>
			
			<div class="section">
				<div class="left">in</div>
				<div class="fields">
				   <form:select path="country" tabindex="2"  onchange="setDefaultUniversity();">
				    				                         
										<form:options items="${country}" itemLabel="value" itemValue="key" />
									</form:select> &nbsp;and who</div>
			</div>
			
	<div class="section">
		<div class="left">attend</div>
		<div class="fields">
			<form:input  path="universityName" maxlength="200" onfocus="UniversityName();" onkeyup="ajax_showOptions(this,'autoCompleteUniversitySearch',event);" tabindex="3"/>&nbsp;and who are
		</div>
   </div>	

   <div style="clear:both"></div>
			<div class="section">
				<div class="left">studying</div>
			  <div class="fields">
					<form:input path="subjectArea" maxlength="80" onfocus="SubjectArea();" onkeyup="ajax_showOptions(this,'autoCompleteSubjectSearch',event);" tabindex="4"/> , who will
			  </div>
	</div>
<div style="clear:both"></div>  
			

			<div class="section">
				<div class="left">achieve</div>
				<div class="fields">
				<form:select path="qualification" id="combo1" tabindex="5">
				  	<form:option value="select">All Qualification Types</form:option>
										<form:options items="${qualification}" itemLabel="value" itemValue="key"  />
									</form:select> &nbsp;and whose notes</div>
			</div>
			
			<div class="section">
				<div class="left">are from</div>
				<div class="fields">
				  	<form:select path="yearLevel" id="combo2" tabindex="6">
				  	<form:option value="select">All Year Levels</form:option>
										<form:options items="${yearLevel}" itemLabel="value" itemValue="key" />
									</form:select> 
				  	, but only if the notes</div>
			</div>
			<div class="section">
				<div class="left"> are of </div>
				<div class="fields">
				  <form:select path="specificType" id="combo3" tabindex="7">
				  	<form:option value="select">All Types</form:option>
										<form:options items="${specificType}" itemLabel="value" itemValue="key" />
									</form:select> &nbsp;and
			  </div>
			</div>
			
			<div class="ending">
				<div class="header6"> So please submit my <input name="" type="submit" title="Search" class="btn_blue" value="Search" tabindex="8" /> &nbsp;for me now!</div>
					<div style="clear:both"><img src="images/spacer.gif" height="6px" /></div>
					<div class="header6">Thanks&nbsp;<%String uname=(String)SessionManager.getSessionAttribute(SessionAttributeKey.tempName, request);if(uname==null){}else { %>
					<%=(String)SessionManager.getSessionAttribute(SessionAttributeKey.tempName, request)%>
					<%} %></div>
			</div>
		</div>
  </div>
<%@ include file="/jsp/footer.jsp" %>
</div>
</form:form>
</body>
</html>