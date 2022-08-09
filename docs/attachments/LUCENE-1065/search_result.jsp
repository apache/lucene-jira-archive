<%/* --------------------------------------------------------------------------------------------
 Hurix Systems Pvt. Ltd.

 Version Information :
 
 Version Number		    : 1.00
 Coded by				: Muarali Krishna.S
 Release Date			: 
 
 -------------------------------- Description ---------------------------------------------------
This jsp is used to display Search Results page. 
 ---------- Change Log -------------------------------------------------------------------------
 Version			:
 Name			:
 Date				:
 Description	:
 -------------------------------------------------------------------------------------------- */%><!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<%@ taglib prefix="fmt" uri="/tld/fmt.tld"%>
<head>
<%@ taglib uri="http://displaytag.sf.net" prefix="display" %>
<%@ page import="java.util.ArrayList,java.util.TreeMap,java.util.Iterator,com.mgh.sps.common.dto.ResourceDTO,com.Ostermiller.util.RandPass,com.mgh.sps.common.util.*" %>
<%@ taglib prefix="spring" uri="/tld/spring.tld" %>
<%@ taglib prefix="c" uri="/tld/c.tld" %>
<%@ taglib prefix="form" uri="/tld/spring-form.tld" %>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
<META HTTP-EQUIV="CACHE-CONTROL" CONTENT="NO-CACHE">
<META HTTP-EQUIV="EXPIRES" CONTENT="01 Jan 1970 00:00:00 GMT">
<META HTTP-EQUIV="PRAGMA" CONTENT="NO-CACHE">
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
		width:300px;	/* Width of box */
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

	.lowerrow{
	background-color:#FFF;
	background: #FFFFFF;

	}
	
	form{
		display:inline;
	}
</style>

<script type="text/javascript">
	var browser_find;
	browser_version= parseInt(navigator.appVersion);
	browser_type = navigator.appName;
	if (navigator.appVersion.indexOf("PPC Mac OS X")!=-1 || navigator.appVersion.indexOf("Intel Mac OS X")!=-1){
			if (-1 != navigator.userAgent.indexOf("Safari")){
					browser_find="macsafari";
				}
		}else{
			if (navigator.appVersion.indexOf("MSIE")!=-1){
					browser_find="iewin";
			}else if (-1 != navigator.userAgent.indexOf("Safari")){
					browser_find="safari";
			}else if (-1 != navigator.userAgent.indexOf("Mozilla")){ 
					browser_find="mozilla";
			}else{
					browser_find="other";
			}
		}
</script>

<script type="text/javascript">
if(browser_find == "mozilla"){	
	document.write('<style type="text/css"> #ajax_listOfOptions{ margin-top:10px; margin-left:137px; } </style>');
}
if(browser_find == "macsafari"){	
	document.write('<style type="text/css"> #ajax_listOfOptions{ margin-top:12px; margin-left:125px; } </style>');
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
			
			//newly added code for country and university dependency
			var changeObj=new changedObject(inputObj+"countryId="+country)
			
			ajax_list_objects[ajaxIndex].requestFile = url;	// Specifying which file to get
			ajax_list_objects[ajaxIndex].onCompletion = function(){ ajax_option_list_showContent(ajaxIndex,changeObj,paramToExternalFile,tmpIndex); };	// Specify function that will be executed after file has been found
			ajax_list_objects[ajaxIndex].runAJAX();		// Execute AJAX function				
		}
		
			
	}
	//newly added code for country and university dependency
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
document.form1.subjectArea.value="a specific SUBJECT AREA";
document.form1.qualification.value='select';
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
	location.href="inProcessLoginnlu.htm?id=searchresults";
}
function home()
{
 location="login.action";
}
function searchTips()
{
 location.href="hello.htm?id1=searchTips";
}
function postRequestSecond()
{
 location.href="postRequest.htm?id=fileRequest";
}
function setDefaultUniversity()
{
 
document.form1.universityName.value="a specific UNIVERSITY";
}
<%@ include file="/js/headerFunctions.js" %>
</script>

</head>
<body>

<form:form name="form1" commandName="command">
<div id="wrapper">
<%@ include file="/jsp/header.jsp" %>
   <%@ page import= "com.mgh.sps.common.util.SessionManager,com.mgh.sps.common.util.SessionAttributeKey" %>
 <div id="headerCrumb">
    <div class="floatL"><img src="images/logo_en_uk.gif" title="GradeGuru, note sharing by students for students" /></div>
  <div class="crumb">
  <%if(id==null){%>
  <div class="crumbLink">&nbsp; <!-- <a href="javascript:logout()"><fmt:message key="search.lable.home"/></a> > <fmt:message key="search.lable.search"/> --> </div>
  <%}else {%>
   <div class="crumbLink">&nbsp; <!-- <a href="javascript:loggedHome()"><fmt:message key="search.lable.home"/></a> > <fmt:message key="search.lable.search"/> --> </div>
   <%}
  if(id==null){
  %> <div class="welcomeName">
  <a href="javascript:inprocessLogin()" class="logout_global"><fmt:message key="search.lable.login"/></a>
  </div><%}else {%>
   <div class="welcomeName"> 
    <fmt:message key="search.lable.welcome"/> <span class="guruName"><%=(String)SessionManager.getSessionAttribute(SessionAttributeKey.tempName, request)%></span>  
    <a href="javascript:logout()" class="logout_global"><fmt:message key="search.lable.logout"/></a>
   </div>
   <%}%>
    <font size="2"  color="red"><form:errors path="*" /></font>
  </div>
 </div>
 
 <div style="clear:both"></div>
 
 <div id="body">
  
  <div>
   <div class="blockHeader"><div class="floatL"><fmt:message key="search.lable.Search"/></div><div class="floatR"><a href="javascript:dummy()" title="Coming soon: refer to the GradeGuru Site Updates on your home page"><fmt:message key="search.lable.searchtips"/></a></div></div>
   <div class="searchHand">
		<div style="display:block;">
			<fmt:message key="search.lable.searchresults_msg1"/>   
			<form:input path="keywords" size="50" maxlength="80" onfocus="KeyWords();" tabindex="1" />
			<fmt:message key="search.lable.searchresults_msg2"/> 
			<form:select path="country" tabindex="2" onchange="setDefaultUniversity();">
			
			<form:options items="${country}" itemLabel="value"  itemValue="key" />
			</form:select>
		</div>
    
	  <div style="float:left; display:inline;">
			<fmt:message key="search.lable.searchresults_msg3"/> 
			<form:input  path="universityName" maxlength="200" size="45" onkeyup="ajax_showOptions(this,'autoCompleteUniversitySearch',event);" onfocus="UniversityName();" tabindex="3"/>
	  </div>
		
	  <div style="float:right; display:inline;">
			 &nbsp;, studying 
			 <form:input path="subjectArea" maxlength="80" size="45" onkeyup="ajax_showOptions(this,'autoCompleteSubjectSearch',event);" onfocus="SubjectArea();" tabindex="4"/>				
      </div> 

    <div style="float:left; display:inline;">
     <fmt:message key="search.lable.searchresults_msg4"/> 
     <form:select path="qualification" id="combo1" tabindex="5">
       <form:option value="select" >All Qualification Types</form:option>
          <form:options items="${qualification}" itemLabel="value" itemValue="key" />
         </form:select>
    </div>
	<div style="float:right; display:inline;" id="combo3">
      <fmt:message key="search.lable.searchresults_msg5"/> 
       <form:select path="yearLevel" tabindex="6">
       <form:option value="select">All Year Levels</form:option>
          <form:options items="${yearLevel}" itemLabel="value" itemValue="key" />
         </form:select> 
      &nbsp;,
    </div>
<div><img src="images/spacer.gif" height="1px" width="420px;" /></div>
    <div style="float:left; display:inline;">
     <fmt:message key="search.lable.searchresults_msg6"/> 
     
     <form:select path="specificType" id="combo2" tabindex="7">
       <form:option value="select">All Types</form:option>
          <form:options items="${specificType}" itemLabel="value" itemValue="key" />
         </form:select>
     &nbsp;.
     <input name="" type="submit" value="Search again! >" class="btn_blue" title="Search again!" style="font-size:13px; margin:1px;" tabindex="8"/>
    </div>
</div>

<div style="clear:both; height:5px"></div>
  <div>
   <%if(null!=(ArrayList)SessionManager.getSessionAttribute(SessionAttributeKey.test,request)){
   ArrayList numberOfFiles =(ArrayList)SessionManager.getSessionAttribute(SessionAttributeKey.test,request);
	if(id==null){}else {if(numberOfFiles.size() >= 60){} else {%>   
   <!--    COMMENTED FOR STUBBING -->  <!--   
   <a href="javascript:postRequestSecond()" class="redLink">Click here to <strong>request notes</strong> if you did not find what you need</a>
   
--> <!-- END OF STUBBING -->
 <a href="javascript:dummy()"  class="redLink" title="Coming soon: refer to the GradeGuru Site Updates on your home page">Click here to <strong>request notes</strong> if you did not find what you need</a> 
   <%}}}%>     
</div> 
 </form:form>

   <form:form name="form2">
    <% if(null==(ArrayList)SessionManager.getSessionAttribute(SessionAttributeKey.test,request)) {%>
	Please try your search again with other similar keywords, or click on request notes to send other members a message that you are looking for notes of this type
	<%} else {%>
     <%
        ArrayList list =(ArrayList)SessionManager.getSessionAttribute(SessionAttributeKey.test,request);
        String str="";
		if(null!= list){
        TreeMap value =null; 
        ArrayList list2 = new ArrayList();
    	Iterator itr=list.iterator();
    		while(itr.hasNext())
     		{
     value = new TreeMap();     
     ResourceDTO dto1=(ResourceDTO)itr.next();
      
	String univer;
	if(dto1.getUniversityName().length()>50) {
	univer=(dto1.getUniversityName()).substring(0,40)+"<br>"+(dto1.getUniversityName()).substring(40,48)+ "..";
	} else if(dto1.getUniversityName().length()>40 && dto1.getUniversityName().length()<=50) {
	univer=(dto1.getUniversityName()).substring(0,40)+"<br>"+(dto1.getUniversityName()).substring(40,dto1.getUniversityName().length());
	} else {
	univer=dto1.getUniversityName();
	}
				
	

	String topicname;
	if(dto1.getTopicsCovered().length()>56) {
	topicname=(dto1.getTopicsCovered()).substring(0,57)+"..." ;
	} else {
	topicname=dto1.getTopicsCovered();
	}

	String description;
	if(dto1.getShortDescription().length()>56) {
	description=(dto1.getShortDescription()).substring(0,57)+"..." ;
	} else {
	description=dto1.getShortDescription();
	}
				
	String qandc;
	if(dto1.getQualificationName().length()>70)	{
	qandc=(dto1.getQualificationName()).substring(0,20)+"<br>"+(dto1.getQualificationName()).substring(20,42)+"<br>"+(dto1.getQualificationName()).substring(42,64)+"<br>"+(dto1.getQualificationName()).substring(64,68)+"..";
	} else if(dto1.getQualificationName().length()>64 && dto1.getQualificationName().length()<=70) {
	qandc=(dto1.getQualificationName()).substring(0,20)+"<br>"+(dto1.getQualificationName()).substring(20,42)+"<br>"+(dto1.getQualificationName()).substring(42,64)+"<br>"+(dto1.getQualificationName()).substring(64,dto1.getQualificationName().length()) ;
	} else if(dto1.getQualificationName().length()>42 && dto1.getQualificationName().length()<=64) {
	qandc=(dto1.getQualificationName()).substring(0,20)+"<br>"+(dto1.getQualificationName()).substring(20,42)+"<br>"+(dto1.getQualificationName()).substring(42,dto1.getQualificationName().length());
	} else if(dto1.getQualificationName().length()>20 && dto1.getQualificationName().length()<=42) {
	qandc=(dto1.getQualificationName()).substring(0,20)+"<br>"+(dto1.getQualificationName()).substring(20,dto1.getQualificationName().length());
	} else{
	qandc=dto1.getQualificationName();
	}

	String contri;
	if(dto1.getUserName().length()>11) {
	contri=(dto1.getUserName()).substring(0,6)+"...";
	} else {
	contri=dto1.getUserName();
	}

	String useful1;
	if(dto1.getUsefulForName().length()>40 ) {
	useful1=(dto1.getUsefulForName()).substring(0,38)+ "..."+ "</span>" ;
	}  else {
	useful1=dto1.getUsefulForName();
	}

				
	String module;
	if(dto1.getModuleName().length()>40 ) {
	module=(dto1.getModuleName()).substring(0,38)+ "..."+ "</span>" ;
	}  else {
	module=dto1.getModuleName();
	}

				
	String year;
	if(dto1.getYearLevelName().length()>40 ) {
	year=(dto1.getYearLevelName()).substring(0,20)+"<br> <span style=\"  padding-left:76px;\">"+(dto1.getYearLevelName()).substring(20,38)+ ".."+ "</span>" ;
	} else if(dto1.getYearLevelName().length()>20 && dto1.getYearLevelName().length()<=40) {
	year=(dto1.getYearLevelName()).substring(0,20)+"<br> <span style=\"  padding-left:76px;\">"+(dto1.getYearLevelName()).substring(20,dto1.getYearLevelName().length())+ "</span>" ;
	} else {
	year=dto1.getYearLevelName();
	}

	String semester;
	if(dto1.getSemester().length()>40 ) {
	semester=(dto1.getSemester()).substring(0,20)+"<br> <span style=\"  padding-left:76px;\">"+(dto1.getSemester()).substring(20,38)+ ".."+ "</span>" ;
	} else if(dto1.getSemester().length()>20 && dto1.getSemester().length()<=40) {
	semester=(dto1.getSemester()).substring(0,20)+"<br> <span style=\"  padding-left:76px;\">"+(dto1.getSemester()).substring(20,dto1.getSemester().length())+ "</span>" ;
	} else {
	semester=dto1.getSemester();
	}


     String fileFormat =(String)dto1.getFileFormat();
     String basePath =ContentContext.getBaseresourcePath();
	 String perpath=ContentContext.getResourcePath();
	 fileFormat=fileFormat.replaceAll(basePath,perpath);
	// session.setAttribute("FilePath",fileFormat);
	 SessionManager.setSessionAttribute(SessionAttributeKey.FilePath,fileFormat,request);
	String encryptPwd=Utilities.encodeToHexString(	new CryptoManager().encrypt3DESCBC("passcode_key",fileFormat.getBytes()));
	int resourceId = FileUtils.getFileId(fileFormat);
	int fileId=Integer.parseInt(dto1.getFileFormatId());
	str=str+","+String.valueOf(resourceId);

	String file1;
String filetwo=(String)SessionManager.getSessionAttribute(SessionAttributeKey.tempName, request);
		 String two=null;
			 if(filetwo!=null)
				{
		two="<a href='FileUsageMediator.htm?id="+encryptPwd+"&ff="+resourceId+"'>";
				}
				else
				{
	  two="<a href='FileUsageMediatornlu.htm?id="+encryptPwd+"&ff="+resourceId+"'>";
				}

	if(dto1.getFileFormatName().length()>58 ) {
	file1=two+(dto1.getFileFormatName()).substring(0,52)+"..."+ "</a>" ;
	} 
	else {
	file1=two+dto1.getFileFormatName()+"</a>";
	}

	String file2;
String filethree=(String)SessionManager.getSessionAttribute(SessionAttributeKey.tempName, request);
		 String three=null;
			 if(filethree!=null)
				{
		three="<a href='FileUsageMediator.htm?id="+encryptPwd+"'>";
				}
				else
				{
	  three="<a href='FileUsageMediatornlu.htm?id="+encryptPwd+"'>";
				}
	if(dto1.getFileFormatName().length()>58 ) {
	file2=three+(dto1.getFileFormatName()).substring(0,52)+"..."+ "</a>&nbsp; <img src='images/icon_pencil.gif' border='0'/> " ;
	}  else {
	file2=three+dto1.getFileFormatName()+"</a>&nbsp; <img src='images/icon_pencil.gif' alt='notes are handwritten' border='0'/> ";
	}
//code from here is moved to below section
	 if(fileId==1) { 
	value.put("universityName",univer);
	 } else {
    value.put("universityName",univer);
	 }
     
	String fileone=(String)SessionManager.getSessionAttribute(SessionAttributeKey.tempName, request);
		 String one=null;
			 if(fileone!=null)
				{
 	one="<a href='userprofile.htm?id="+contri+"'>"+contri+"</a>";
				}
				else
				{
	  one="<a href='userprofilenlu.htm?id="+contri+"'>"+contri+"</a>";
				}

    value.put("qualCourse",qandc+"");
    value.put("uname",one);


	//This is added for changing the ui acording to wireframes
	 if(fileId==1) { 
	value.put("rating","Coming soon</tr><tr ><td style=\"background: white\" colspan=\"4\"><span style=\"color: #006699; padding-left:7px;\"> File Name:&nbsp;&nbsp;&nbsp;</span> "+"<b>"+file2+"</b>"+ "</td></tr>"+
						"<tr><td  style=\"background: white\" colspan=\"2\"><span style=\"color: #006699;padding-left:13px;\"> Usefulfor:&nbsp;&nbsp;&nbsp;</span>"+useful1+ "</td><td  style=\"background: white\" colspan=\"2\"><span style=\"color: #006699; padding-left:38px;\"> Year :&nbsp;&nbsp;&nbsp;</span>"+year+ "</td></tr>"	+
					"<tr><td  style=\"background: white\" colspan=\"2\"><span style=\"color: #006699; padding-left:23px;\"> Module:&nbsp;&nbsp;&nbsp;</span>"+module+ "</td><td  style=\"background: white\" colspan=\"2\"><span style=\"color: #006699;padding-left:10px;\">  Semester :&nbsp;&nbsp;&nbsp;</span>"+semester+"</td></tr>"	+
		"<tr><td  style=\"background: white\" colspan=\"4\" nowrap><span style=\"color: #006699;padding-left:25px;\"> Topics:&nbsp;&nbsp;&nbsp;</span>"+topicname+ "</td></tr>"	+
		"<tr><td  style=\"background: white\" colspan=\"4\" nowrap><span style=\"color: #006699; padding-left:-1px;\"> Description:&nbsp;&nbsp;&nbsp;</span>"+description+ "</td></tr>");
 } else {
	value.put("rating","Coming soon</tr><tr ><td  style=\"background: white\"   colspan=\"4\"><span style=\"color: #006699; padding-left:7px;\"> File Name:&nbsp;&nbsp;&nbsp;</span> "+"<b>"+file1+"</b>"+"</td></tr>"+
						"<tr><td  style=\"background: white\" colspan=\"2\"><span style=\"color: #006699;padding-left:13px;\"> Usefulfor:&nbsp;&nbsp;&nbsp;</span>"+useful1+ "</td><td  style=\"background: white\" colspan=\"2\"><span style=\"color: #006699; padding-left:38px;\">  Year :&nbsp;&nbsp;&nbsp;</span>"+year+ "</td></tr>"	+
					"<tr><td  style=\"background: white\" colspan=\"2\"><span style=\"color: #006699; padding-left:23px;\"> Module:&nbsp;&nbsp;&nbsp;</span>"+module+ "</td><td  style=\"background: white\" colspan=\"2\"><span style=\"color: #006699;padding-left:10px;\">  Semester :&nbsp;&nbsp;&nbsp;</span>"+semester+"</td></tr>"	+
		"<tr><td  style=\"background: white\" colspan=\"4\" nowrap><span style=\"color: #006699;padding-left:25px;\"> Topics:&nbsp;&nbsp;&nbsp;</span>"+topicname+ "</td></tr>"	+
		"<tr><td  style=\"background: white\" colspan=\"4\" nowrap><span style=\"color: #006699; padding-left:-1px;\"> Description:&nbsp;&nbsp;&nbsp;</span>"+description+ "</td></tr>");
	
	}
     list2.add(value);        
     }    
  request.setAttribute( "test", list2 );    
	}else{ %>
	
    <%}%>
   <%   String filetwo=(String)SessionManager.getSessionAttribute(SessionAttributeKey.tempName, request);
		 String requri=null;
			 if(filetwo!=null)
				{
 requri="SearchResults.htm";
				}
				else
				{
	  requri="SearchResultsnlu.htm";
				}
				%>
<br>
  <display:table name="test" class="simple" pagesize="8" requestURI="<%=requri%>" sort="list"  defaultorder="descending">
  
 <display:setProperty name="paging.banner.page.link" value="<a href=\"{1}\">{0}</a>" /><!-- added for removing alttext-->
 
   <display:setProperty  name="paging.banner.page.separator" > </display:setProperty>
   
   <display:setProperty  name="paging.banner.placement">both</display:setProperty>
    
    <display:setProperty name="paging.banner.full">
 		<div class="pagelinks"  align="right">Page <a href="{2}">Previous</a> {0}  <a href="{3}">Next</a></div>	
		<div><img src="images/spacer.gif" height=25px></div>
 	</display:setProperty>
 		
 	<display:setProperty name="paging.banner.first">
 		<div class="pagelinks"  align="right" >Page <a href="{2}">Previous</a> {0}  <a href="{3}">Next</a></div>
		<div><img src="images/spacer.gif" height=25px></div>
 	</display:setProperty>
 		
 	<display:setProperty name="paging.banner.last"> 
 		<div class="pagelinks"  align="right" >Page <a href="{2}">Previous</a> {0}  <a href="{3}">Next</a></div>
		<div><img src="images/spacer.gif" height=25px></div>
	</display:setProperty>
		
	<display:setProperty name="paging.banner.onepage"> 
 		<div class="pagelinks"  align="right" > </div>				
		<div><img src="images/spacer.gif" height=25px></div>
	</display:setProperty>	

	<display:setProperty name="paging.banner.no_items_found"> 
 		<div class="pagebanner"  align="left" ><b>&nbsp;&nbsp;Search results </b> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;No files found </div>				
	</display:setProperty>

	<display:setProperty name="paging.banner.one_item_found"> 
 		<div class="pagebanner"  align="left" ><b>&nbsp;&nbsp;Search results </b> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1 file found </div>				
	</display:setProperty>

	<display:setProperty name="paging.banner.all_items_found" >				
		<div class="pagebanner"  ><b>&nbsp;&nbsp;Search results </b> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;{0} files found </div>
	</display:setProperty>
 		    
	<display:setProperty name="paging.banner.some_items_found" >				
 		<div class="pagebanner"><b>&nbsp;&nbsp;Search results </b> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;{0} files found
		</div>
 	</display:setProperty>    
    
      
   <display:column property="universityName" style="width:340px;" title="University" sortable="true" headerClass="sortable" />
   <display:column property="qualCourse" title="Qualification & Course" sortable="true" headerClass="sortable" />
   <display:column property="uname" title="Contributor"  sortable="true" headerClass="sortable"/>
   <!--    FOR adding new property --> 
   <display:column property="rating" title="Rating" sortable="true" headerClass="sortable" > 
    <!--    COMMENTED FOR STUBBING -->  <!--   
    <img src="images/star_4.gif"/><br><br><br><br><br><br><br></br>
    --> <!-- END OF STUBBING -->
 
   </display:column>  
  </display:table>  
 <div style="clear:both"></div>    
   <div>
<%String id1=(String)SessionManager.getSessionAttribute(SessionAttributeKey.tempName, request);
if(null!=(ArrayList)SessionManager.getSessionAttribute(SessionAttributeKey.test,request)){
 ArrayList totalFiles =(ArrayList)SessionManager.getSessionAttribute(SessionAttributeKey.test,request);
if(id1==null){}else {if(totalFiles.size() >= 60){} else {%>
     <!--    COMMENTED FOR STUBBING -->  <!--   
     <p><a href="javascript:postRequestSecond()" class="redLink">Click here to <strong>request notes</strong> if you did not find what you need</a></p>
     
--> <!-- END OF STUBBING -->
 <p><a href="javascript:dummy()" class="redLink" title="Coming soon: refer to the GradeGuru Site Updates on your home page">Click here to <strong>request notes</strong> if you did not find what you need</a></p>
     <%}}}%> 
   </div>  
<div class="fullBlueLine"><img src="images/spacer.gif" /></div>
 <div id="footer">
		 <%String id6=(String)SessionManager.getSessionAttribute(SessionAttributeKey.tempName, request);
			%> 
  		<%if(id6==null){%>
		<a href="communitystandardmessagenlu.htm">Community Standards</a> 
		 <% } else{%>
		 <a href="communitystandardmessage.htm?sid=loggedInUser">Community Standards</a> 
		 <%}%> &nbsp;| &nbsp;
		 <%String id7=(String)SessionManager.getSessionAttribute(SessionAttributeKey.tempName, request);
			%> 
		 <%if(id7==null){%>
		<a href="privacystatementnlu.htm">Privacy Statement</a> 
		 <% } else{%>
		<a href="privacystatement.htm?sid=loggedInUser">Privacy Statement</a> 
		 <%}%>&nbsp;| &nbsp;
		<%String contactus=(String)SessionManager.getSessionAttribute(SessionAttributeKey.tempName, request);
					%>
					 <%if(contactus==null){%>
					<a href="contactusnlu.htm" >Contact Us</a>
					 <% } else{%>
					<a href="contactus.htm" >Contact Us</a>
					<%}%> &nbsp;| &nbsp;
		<%String id4=(String)SessionManager.getSessionAttribute(SessionAttributeKey.tempName, request);
	%>
		  <%if(id4==null){%>
		<a href="helpmessagenlu.htm">Help</a>
		 <% } else{%>
		<a href="helpmessage.htm?sid=loggedInUser">Help</a> 
		<%}%>&nbsp;| &nbsp;
		<!--<a href="javascript:reportAbuse()">Report Disrespect</a> &nbsp;| &nbsp;
		-->
		<%String id5=(String)SessionManager.getSessionAttribute(SessionAttributeKey.tempName, request);
	%>
		  <%if(id5==null){%>
		<a href="reportdisrespectnlu.htm?id=<%=str%>">Report Disrespect</a>
		<% } else{%>
		<a href="reportdisrespect.htm?id=<%=str%>">Report Disrespect</a>
		<%}%>&nbsp;| &nbsp;
		<!--    COMMENTED FOR STUBBING -->  <!-- 
		<a href="sitemapnlu.htm">Site Map</a> &nbsp;| &nbsp;   
		<a href="makesuggestionnlu.htm">Make A Suggestion</a> &nbsp;| &nbsp;
		<a href="invitefriendnlu.htm">Invite A Friend</a>	 
	   --> <!-- END OF STUBBING -->
	   <a href="javascript:dummy()" title="Coming soon: refer to the GradeGuru Site Updates on your home page">Site Map</a> &nbsp;| &nbsp; 
	   	<a href="javascript:dummy()"  title="Coming soon: refer to the GradeGuru Site Updates on your home page">Make A Suggestion</a> &nbsp;| &nbsp;
		<a href="javascript:dummy()" title="Coming soon: refer to the GradeGuru Site Updates on your home page">Invite A Friend</a>	 
	</div>
  
	<%}%>

</form:form>
</body>
</html>