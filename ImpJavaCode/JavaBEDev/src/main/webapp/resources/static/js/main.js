/* Initialisers
-------------------------------*/
	$(function() {

		/* Global vars */
			/* media queries */
			mq_red = '(min-width: 1300px)';
			mq_orange = '(min-width: 992px) and (max-width: 1299px)';
			mq_purple = '(max-width: 991px)';
			mq_yellow = '(min-width: 768px) and (max-width: 991px)';
			mq_green = '(max-width: 767px)';
            mq_brown = '(max-width: 599px)';
			mq_blue = '(max-width: 479px)';
			/* animation speed */
			anSp = 500;
			anSpFast = 400;
            
            ww = document.body.clientWidth;
			
			/* navigation */
			isMobileNav = false;
			mobileNav = '';

			/* forms */
			//forms = $('form');
			header = $('header');
			homeTh =$('.th-img-container');
			bannerImgFit =$('.banner-img-fit');
			//horizontalTab =$('#horizontalTab');
			//HeroBanner =$('.f');
			//BookingSelTop = $('.booking-sel-box');
			//AccordionBox = $('.accordion-style');
			ImgFit = $('.img-fit');
			SingleItemCarousel = $('.single-item-carousel');
            TwoItemCarousel = $('.two-item-carousel');
			ThreeItemCarousel = $('.three-item-carousel');
            FourItemCarousel = $('.four-item-carousel');
            monthCarousel = $('.month-carousel');
            CustomField =$('.custom-field');
            UiInputAppendDate = $('.input-append.date').find('input');
           /* AutoComplete =$('.AutoCompleteTags');*/
			AutoComplete =$('#holidayTags,.holidayTags');
			AutoCompleteSelected =$('#holidayTagsSelected,.holidayTagsSelected');
            //ScrollPanel =$('.scrollable-panel');
            UiCustomizeCalender2=$('#datepicker2');
            UiCustomCalender=$('#custom_datepicker, .custom_datepicker');
            PkgThumbnail=$('.offer-ribbon');
            OwlCarousels=$('.owl-carousel');
            SubItinerary =$('.itenary-details');
            accordinCont = $('.accordion-style');
            //FloatingHeights = $('.floating-btns');
            UiCustomizeCalenderMultiCity=$('.datepickerMulticity');
            UiCustomTwoMonthsCalender=$('.CustomTwoMonthsCalendar');
            FixedPkgBar =$('.top-mid-sec');
            //PhotoPanel = $('.photos-panel');
            HorizontalTab = $('#HorizontalTab, #HorizontalTab1');
            checkMQs();
		    checkFeatures();

		/**********/

		if(header.length){initHeader();	}
		if(homeTh.length){inithomeTh();	}
		if(ImgFit.length){initImgFit();}
		if(SingleItemCarousel.length){initSingleItemCarousel();}
        if(TwoItemCarousel.length){initTwoItemCarousel();}
		if(ThreeItemCarousel.length){initThreeItemCarousel();}
		if(FourItemCarousel.length){initFourItemCarousel();}
		if(monthCarousel.length){initmonthCarousel();}
        if(CustomField.length){initCustomField();}
        if(bannerImgFit.length){initbannerImgFit();}
		//if(PhotoPanel.length){initPhotoPanel();}
        if(UiInputAppendDate.length){initUiInputAppendDate();}
        if(AutoComplete.length){initAutoComplete();}
		if(AutoCompleteSelected.length){initAutoCompleteSelected();}
        //if(ScrollPanel.length){initScrollPanel();}
        if(UiCustomizeCalender2.length){initUiCustomizeCalender2();}
        if(UiCustomCalender.length){initUiCustomCalender();}
        if(PkgThumbnail.length){initPkgThumbnail();}
        if(OwlCarousels.length){initOwlCarousels();}
        if(SubItinerary.length){initSubItinerary();}
        if(accordinCont.length){initAccordinCont();}
        //if(FloatingHeights.length){initFloatingHeights();}
        if(UiCustomizeCalenderMultiCity.length){initUiCustomizeCalenderMultiCity();}
        if(UiCustomTwoMonthsCalender.length){initUiCustomTwoMonthsCalender();}
        if(FixedPkgBar.length){initFixedPkgBar();}
        if(HorizontalTab.length){initHorizontalTab();}
		adjustMenu();
		adjustMenuHov();
		adjustFooter();
		
	});
	
	$(window).resize(function(){
		waitForFinalEvent(function(){
			sizeOrientationChange();
		}, 100, 'main resize');
		adjustMenu();
		adjustMenuHov();
		adjustFooter();
	});

	if (!window.addEventListener) {
		window.attachEvent('orientationchange', sizeOrientationChange);
	}else {
		window.addEventListener('orientationchange', sizeOrientationChange, false);
	}
    $(window).load(function(){
        $('#WebsitePreloader').slideUp('slow');
	});
/*** prepare menu **/
$(".desktopnav").append("<a href='#' class='closeMenu'></a><nav><ul class='mob-menu nav' id='nav-menu'></ul></nav>");
$('#nav_menu > li').clone().appendTo(".desktopnav");
$('#nav_menu').addClass("hidden-xs");

$(".nav > li > a, .mob-menu > li > a").each(function() {
	if ($(this).next().length > 0) {
		$(this).addClass("parent");
	};
})

/*
$(".toggleMenu").click(function(e) {
	e.preventDefault();
	$(this).toggleClass("active");
	$(".desktopnav").slideToggle();
})
*/
$(document).on("click",".share-sec .wishlist", function(e) {
	e.preventDefault();
	$(this).addClass('added');
});

$(".toggleMenu").click(function(e) {
    e.preventDefault();
if($(this).hasClass('active')){
    $(this).removeClass("active");
    $('.desktopnav').fadeOut();
}
    else{
        $(this).addClass("active");
        $('.desktopnav').fadeIn()
    }
});
$(".closeMenu").click(function(e) {
    e.preventDefault();
    $(".toggleMenu").removeClass("active");
    $('.desktopnav').fadeOut();
})

$("html.touch .nav > li > a.parent,.mob-menu > li a.parent").attr({'href':'javascript:void();'});
$('.mob-menu > li a.parent').append('<b class="caret"></b>');
$(".mob-menu > li").unbind('mouseenter mouseleave');

/**** init ImgFIt ***/
var initImgFit = function() {
$(ImgFit).find("figure").find("img").each(function(i, elem) {
  var imgItem = $(elem);
  var div = $("<div />").css({
    background: "url(" + imgItem.attr("src") + ") no-repeat",
	'background-size': "cover",
    'background-position': "center",
    width: "100%",
    height: "100%"
  });
   
  imgItem.replaceWith(div);
});
}


//* init Header****//
var initHeader = function() {

$(".mob-menu > li a.parent").bind('click', function(e) {
	var $thisNav = $(this);
	var thisParentList = $thisNav.parent();
	var thisParent  = $thisNav.parent().find('ul');
	thisParent.slideToggle();
	$('.mob-menu > li ul').not(thisParent).slideUp().removeClass('hover');
	$('.mob-menu > li').not(thisParentList).removeClass('hover');
	$(this).parent("li").toggleClass("hover"); 
});
    /*09-01-2017 : Booking details/summary header static*/
    if(Modernizr.mq()){
        if($(".inner-banner").length){
       	 	$(".inner-pg .wrapper").css({'padding-top':0});
        }
    }
    else{
			$(".header-static.inner-pg .wrapper").css({'padding-top':($("header").outerHeight())});
    }
    /*09-01-2017 : Booking details/summary header static*/

}

/**** init hmoe tabs ***/
var inithomeTh = function() {
$(homeTh).find("img").each(function(i, elem) {
  var img = $(elem);
  var span = $("<div />").css({
    background: "url(" + img.attr("src") + ") no-repeat", 'background-size': "cover",
    width: "100%",
    height: "100%",
	display: "inline-block"
  });
  img.replaceWith(span);
});
}
/****** initHeroBanner *****/
var initHeroBanner = function(){}




//* adjust menu****//
var adjustMenu = function() {
	var $nav_overlay = $('#nav_overlay');
		
	if(Modernizr.mq(mq_green)){
		$(".toggleMenu").css("display", "inline-block");
		if (!$(".toggleMenu").hasClass("active")) {
			$(".mobile-nav").hide();
		} else {
			$(".mobile-nav").show();
		}
         $(".desktopnav").hide();
	}
	else {
		$(".toggleMenu").css("display", "none");
        $(".desktopnav").show();
		if( /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent) ) {
			  $("#nav_menu  > li > a").bind('touchstart mouseenter');
		      $("#nav_menu  > li > a").unbind('click').bind('touchstart mouseenter', function() {
			   $("#nav_menu  > li").removeClass('hover');
		 	  $(this).parent().toggleClass('hover');
			
		   });
			
			}
			else{
				
		      $("#nav_menu > li").removeClass("hover");
			  $("#nav_menu > li > a").unbind('click');
		      $("#nav_menu > li").unbind('mouseenter mouseleave').bind('mouseenter mouseleave touchstart', function() {
		 	  $(this).toggleClass('hover');
			
		     });
			}
	}
};

$('ul#nav_menu > li:has(ul)').addClass('has-submenu');

var adjustMenuHov = function(e) {

	//alert("hi");
	var $nav_menu		= $('#nav_menu');
	var $nav_menu_items	= $nav_menu.find('li.has-submenu');
	var $nav_menu_items_link	= $nav_menu.find('li.has-submenu > a');
	var $nav_overlay		= $('#nav_overlay');
	
	$nav_menu_items.find('ul').hide();
	//e.preventdefault();
	if(Modernizr.mq(mq_purple)){
	//$nav_menu_items.find('ul').css('z-index','-1').show();
	$nav_overlay.hide();
	 //$('header').removeClass('sec-hove');
	}
	
   else {
		
	$nav_menu_items.bind('touchstart mouseenter',function(){
	
	 //alert("hi");
		var $this = $(this);
		$this.addClass('slided selected');
		$this.find('ul').css('z-index','9999').stop(true,true).slideDown(200,function(){
			$nav_menu_items.not($this).find('ul').hide();
			//$this.removeClass('slided');
			
		$nav_overlay.stop(true,true).fadeTo(200, 0.3);
		$this.addClass('hovered');
		$('header').addClass('sec-hove');
		
		
		});
	}).bind('mouseleave',function(){
		var $this = $(this);
		$this.removeClass('selected').find('ul').css('z-index','1').slideUp(200,function(){
		$this.removeClass('slided selected');
		$this.removeClass('hovered');
		$nav_overlay.stop(true,true).fadeTo(200, 0, function(){
		$('header').removeClass('sec-hove');
		$nav_overlay.hide();
	});});
	})			
	$('#nav_menu li.has-submenu ul').css({"display":"none"});
	}		
}
			
var adjustFooter = function() {}

var ChangeView = function(){}

var initAccordionBox = function(e) {}

/* Functions
-------------------------------*/

	
/* Utility
-------------------------------*/
	// fired on window orientation or size change
	function sizeOrientationChange(){
		checkMQs();
	}

	// check media query support,
	// if supported, set variables
	// if not, set 'orange' as default
	function checkMQs(){
		// returns 'true' if MQs are supported
		if(Modernizr.mq('only all')){
			mq_red_check = Modernizr.mq(mq_red);
			mq_orange_check = Modernizr.mq(mq_orange);
			mq_yellow_check = Modernizr.mq(mq_yellow);
			mq_green_check = Modernizr.mq(mq_green);
			mq_blue_check = Modernizr.mq(mq_blue);
			mq_purple_check = Modernizr.mq(mq_purple);
		}else{
			mq_red_check = false;
			mq_orange_check = true;
			mq_yellow_check = false;
			mq_green_check = false;
			mq_blue_check = false;
			mq_purple_check = false;
		}
		
		// call responsive nav (no init)
		//responsiveNav();
	}
	
	//Check device features
	function checkFeatures(){
		//touch devices
		touchEnabled = Modernizr.touch;		

		if(touchEnabled){
			//ensures that touch devices are still able to scroll up/down smoothly
			$('html, body').removeClass('no-touch').addClass('touch-mod');
		}
		
		//placeholder support
		formPlaceholders = Modernizr.input.placeholder;

		//drop shadow support
		boxShadows = Modernizr.boxshadow;

		// IE7 flag (used to disabled tours slider)
		isIE7 = $('html').hasClass('ie7');

		// IE8 flag
		isIE8 = $('html').hasClass('ie8');
		
		// initialise forms if they exist
/*	if(forms.length){
		initForms();	
	}*/

	}
	
	// waits for final event to avoid multi-firing (ie: using window.resize)
	// originally from: http://stackoverflow.com/questions/2854407/javascript-jquery-window-resize-how-to-fire-after-the-resize-is-completed
	var waitForFinalEvent = (function () {
		var timers = {};
		return function (callback, ms, uniqueId) {
			if (!uniqueId) {
				uniqueId = "Don't call this twice without a uniqueId";
			}
			if (timers[uniqueId]) {
				clearTimeout (timers[uniqueId]);
			}
			timers[uniqueId] = setTimeout(callback, ms);
		};
	})();
	
function setLocation(url){
	window.location.href = url
}

/*** EQUAL HEIGHTS ***/
var $window = $(window);
//	

equalheight = function(container){
var currentTallest = 0,
	currentRowStart = 0,
	rowDivs = new Array(),
	$elm,
	topPosition = 0;
 $(container).each(function() {

   $elm = $(this);
   $($elm).height('auto')
   topPostion = $elm.position().top;

   if (currentRowStart != topPostion) {
     for (currentDiv = 0 ; currentDiv < rowDivs.length ; currentDiv++) {
       rowDivs[currentDiv].height(currentTallest);
     }
     rowDivs.length = 0; // empty the array
     currentRowStart = topPostion;
     currentTallest = $elm.height();
     rowDivs.push($elm);
   } else {
     rowDivs.push($elm);
     currentTallest = (currentTallest < $elm.height()) ? ($elm.height()) : (currentTallest);
  }
   for (currentDiv = 0 ; currentDiv < rowDivs.length ; currentDiv++) {
     rowDivs[currentDiv].height(currentTallest);
   }
 });

    
    
}

$(window).load(function() {
	equalheight('.equal-heights > div');
    equalheight('.equal-heights > li');
});

$(window).resize(function(){
	equalheight('.equal-heights > div');
    equalheight('.equal-heights > li');
});

if($('.recommendeds').length){
var largest = 0; //start with 0
$(".recommendeds #recommend-pkg-tabs > li h3").each(function(){ //loop through each section
   var findHeight = $(this).height(); //find the height
   if(findHeight > largest){ //see if this height is greater than "largest" height
      largest = findHeight; //if it is greater, set largest height to this one 
   }  
});

$(".recommendeds #recommend-pkg-tabs > li h3").css({"height":largest+"px"});
}


/*** ScrollTop ***/
$(".totop").hide();

$(function(){
$(window).scroll(function(){
if ($(this).scrollTop()>300){
	$('.totop').slideDown();
} 
else{
	$('.totop').slideUp();
}
});

$('.totop a').click(function (e){
e.preventDefault();
$('body,html').animate({scrollTop: 0}, 500);
});
});


/*** Header Fixed After Scroll ***/
$(window).scroll(function(){
	if ($(this).scrollTop()>50){
		$('header').addClass('fixedHeader');
	} 
	else{
		$('header').removeClass('fixedHeader');
	}
});

/** Template Animations **/
jQuery(window).ready(function () {theme.init();});
jQuery(window).load(function () {theme.initAnimation(); });

var theme = function () {
    // prevent empty links
    // ---------------------------------------------------------------------------------------
    function handlePreventEmptyLinks() {
        $('a[href=#]').click(function (event) {
            event.preventDefault();
        });
    }

    // Scroll totop button
    // ---------------------------------------------------------------------------------------
    function handleToTopButton() {
        $(window).scroll(function () {
            if ($(this).scrollTop() > 10) {
                $('.to-top').css({bottom:'15px'});
            } else {
                $('.to-top').css({bottom:'-100px'});
            }
        });
        $('.to-top').click(function () {
            $('html, body').animate({scrollTop:'0px'}, 800);
            return false;
        });
    }
    
    // INIT FUNCTIONS
    // ---------------------------------------------------------------------------------------
    return {
        onResize:function() {
            resizePage();
        },
        init:function () {
            handleToTopButton();
        },
        // Animation on Scroll
        initAnimation:function () {
            var isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
            if (isMobile == false) {
                $('*[data-animation]').addClass('animated');
                $('.animated').waypoint(function (down) {
                    var elem = $(this);
                    var animation = elem.data('animation');
                    if (!elem.hasClass('visible')) {
                        var animationDelay = elem.data('animation-delay');
                        if (animationDelay) {
                            setTimeout(function () {
                                elem.addClass(animation + ' visible');
                            }, animationDelay);
                        } else {
                            elem.addClass(animation + ' visible');
                        }
                    }
                }, {
                    offset:$.waypoints('viewportHeight')-60
                });
            }
        }
    }
}();

/** Compare **/
$('.pkg-listing > li .compare-check').find('input[type=checkbox]').on('ifChecked', function(){
   var targeTrip = $(this).closest('li'),
       TripTl = $(this).closest('.compare-check').parent().children('.pkg-thumbnail').find('.pkg-tl'),
       Tripname = $(this).closest('.compare-check').parent().children('.pkg-thumbnail').find('.pkg-tl').text(),
       TripId = $(this).closest('.compare-check').parent().children('.pkg-thumbnail').find('.pkg-tl').text().toLowerCase().replace(/[\ \*\^\'\!\&]/g, ''),
       TripLength = $('.pkg-listing > li.selected').length;
    
    if(TripLength<3){
        if(TripLength>0){
            $('.cm-mand-txt').hide();
            $('.cm-btn').show();
        }
        else{
             $('.cm-mand-txt').show();
            $('.cm-btn').hide();
        }
        
        $('.compare-list').fadeIn();
        targeTrip.addClass('selected');
       
        
        if(!$(TripTl).attr('rel') == ''){
            var TripTlRel = $(TripTl).attr('rel');
            $('.compare-list .cm-trips').find('li:nth-child('+(TripLength+1)+')').addClass('filled').addClass(TripId).find('b').html(Tripname+'<small>'+TripTlRel+'</small>');
        }
        else{
            $('.compare-list .cm-trips').find('li:nth-child('+(TripLength+1)+')').addClass('filled').addClass(TripId).find('b').text(Tripname);
        }
        
        targeTrip.addClass(TripId); 
    }
    
    else{
        alert('You can choose maximum 3 trips for compare.');
        var cual= this;
        setTimeout(function(){ $(cual).iCheck('uncheck');}, 1);
    } 
 //alert('Check');
}).on('ifUnchecked', function(){

     //$(this).closest('li').removeClass('selected')
     
   var targeTrip = $(this).closest('li'),
       Tripname = $(this).closest('.compare-check').parent().children('.pkg-thumbnail').find('.pkg-tl').text(),
       TripId = $('.cm-trips > .'+ $(this).closest('.compare-check').parent().children('.pkg-thumbnail').find('.pkg-tl').text().toLowerCase().replace(/[\ \*\^\'\!\&]/g, '')),
       
       
       TripLength = $('.pkg-listing > li.selected').length;
    

       // targeTrip.removeClass('selected');
        //alert(TripLength);
    if(!TripLength<2){
        if(!(TripLength>2)){$('.cm-mand-txt').show();$('.cm-btn').hide();}
        if(!(TripLength>1)){$('.compare-list').fadeOut();}
        }
    else{
             if(TripLength>2){
            $('.cm-mand-txt').hide();
            $('.cm-btn').show();
        }
        else{
             $('.cm-mand-txt').show();
            $('.cm-btn').hide();
        }
        }

    // alert('Uncheck');
    //alert(TripId);
    $('.compare-list .cm-trips').find(TripId).find('b').text(''); 

    $(TripId).attr('class', '');
    //$(TripId).removeClass('selected'); 
    targeTrip.removeClass(TripId).removeClass('selected');  
   
});
    
$('.compare-list .btn-remove').click(function(evt1){
    evt1.preventDefault();
    $(this).parent().find('b').text('');
    $(this).parent().removeClass('filled');
    
    var TripId = '.'+ $(this).parent().attr('class');
    //alert(TripId);
    $(TripId).find('input[type=checkbox]').iCheck('uncheck');
    
});
    
$('.compare-list .btn-close').click(function(evt){
    evt.preventDefault();
    $('.compare-list').fadeOut();
    $('.compare-list .cm-trips li').removeClass('filled').find('b').text('');
    $('.pkg-listing > li').removeClass('selected');
    $('.pkg-listing > li').find('input[type=checkbox]').iCheck('uncheck');
});
/*** Compare Ends ***/

/* Share Custom Functionality : Starts */
$(document).on("click",".share-tooltip a", function(e) {
	e.preventDefault();
    $(".share-tooltip a, .tool-tip.info a").removeClass("active");
	$('.share-collapse, .tooltip-cont').hide();
	$('.pkg-listing.list-view > li').css({'z-index':'1'});
	$(this).parents('li').css({'z-index':'1000'});
	$(this).parent().find('.share-collapse').show();
    var containerPos = $(this).parents('.container').offset().left;
    var containerWdth = $(this).parents('.container').outerWidth();
    var ContPos = $(this).closest('.share-tooltip').children('.share-collapse').offset().left;
    var ContWdth=$(this).closest('.share-tooltip').children('.share-collapse').outerWidth();
    var pos=ContPos-containerPos;
    var totWdth=pos+ContWdth;
    var margin=totWdth-containerWdth+30;
    if(totWdth>containerWdth)
    { $(this).closest('.share-tooltip').children('.share-collapse').offset({left:ContPos-margin});}

    $(this).toggleClass("active");
});
/* Share Custom Functionality : Ends */

$("body").click(function(e) {
    if(e.target.className !== "share-collapse") {
		
      $(".share-collapse").hide(); 
      $(".share-tooltip a").removeClass("active");
    }	
    if(e.target.className !== "tooltip-cont") {		
      $(".tooltip-cont").hide(); 
      $(".tool-tip a").removeClass("active");
    }
    
  });
/* Expan Collapse : Starts */
$(document).on("click",".sec-toggle", function(){
    $(this).prev(".details-sec-cont").slideToggle(); 
	$(this).toggleClass("active");
});
/* Expan Collapse : Ends */


/* Custom Trip : Starts */
//DragDrop();
 
$(document).on('click','.ajax-btn', function(evt) {
    evt.preventDefault();
    if($('.droppable-sect').length){$('.droppable-sect').droppable( "destroy" ).removeClass('droppable-sect');}
    $(this).addClass('disabled').closest('.schedule-slot').addClass('selected').children(0).addClass('droppable-sect');
        DragDropHotel();
    
	if(!$(this).attr('data-rel') == ''){
    //var url = '../static/customised-trip/'+$(this).attr("rel")+'.shtml';
        var ClickTarget = '.'+$(this).attr("data-rel");
            
	$("#lt-panels > div").not(ClickTarget).hide();
			$(ClickTarget).show();
			ImgFit =$('.img-fit');
			if(ImgFit.length){initImgFit();}
			if($('.owl-carousel').length){var owl = $('.owl-carousel'); var owlInstance = owl.data('owlCarousel'); if(owlInstance != null) owlInstance.reinit();}
     
        
        $(window).animate({scrollTop:($(ClickTarget).offset().top-150)},500);
        
        //Detail Section Tab
if(ClickTarget=='.activities-details-lt-panel'){
        //alert('ActivityDetailSectionTabs');
    $('#ActivityDetailSectionTabs').responsiveTabs();
    $('#Activitytab-01').responsiveTabs();
    $('#Activitytab-02').responsiveTabs();
}


if(ClickTarget=='.transfer-details-lt-panel'){
        //alert('TransferDetailSectionTabs');
    $('#TransferDetailSectionTabs').responsiveTabs();
    $('#Transfertab-01').responsiveTabs(); 
    $('#Transfertab-02').responsiveTabs();
}

if(ClickTarget=='.hotel-details-lt-panel'){
    $('#HotelDetailSectionTabs').responsiveTabs();
    $('#Hoteltab-01').responsiveTabs();
    $('#Hoteltab-02').responsiveTabs();
    if($('.owl-carousel').length){var owl = $('.owl-carousel'); var owlInstance = owl.data('owlCarousel'); if(owlInstance != null) owlInstance.reinit();}
}

	}
	else{
		$("#lt-panels > div").fadeOut();
        $(".itinerary-lt-panel").fadeIn();

	};
     
});

$(document).on('click','.filter-link', function(evt) {
    var TargetOpen = $(this).attr('href');
	evt.preventDefault();
    //alert(TargetOpen);
    $(TargetOpen).show();
    $('.scrollable-panel').addClass('filter');
    var topPos = $(this).offset().top + $(this).outerHeight();
     $('.filter-sec-on-click').offset({top:topPos});
});

$(document).on('click','.addactivitytransfer-btn', function(evt) {
//$('.addtransfer-btn').on('click', function(evt) {
    evt.preventDefault();
    if($('.droppable-sect').length){$('.droppable-sect').droppable( "destroy" ).removeClass('droppable-sect');}
    $(this).addClass('disabled').closest('.schedule-slot').addClass('selected').children(0).addClass('droppable-sect');
        DragDropActivityTransfer();
    
	if(!$(this).attr('data-rel') == ''){
        var ClickTarget = '.'+$(this).attr("data-rel");
        
	$("#lt-panels > div").not(ClickTarget).hide();
			$(ClickTarget).show();
			ImgFit =$('.img-fit');
			if(ImgFit.length){initImgFit();}
			if($('.owl-carousel').length){var owl = $('.owl-carousel'); var owlInstance = owl.data('owlCarousel'); if(owlInstance != null) owlInstance.reinit();}
        $(window).animate({scrollTop:($(ClickTarget).offset().top-150)},500);
	}
	else{
		$("#lt-panels > div").fadeOut();
        $(".itinerary-lt-panel").fadeIn();
	};   
});

$(document).on('click','.addtransfer-btn', function(evt) {
//$('.addtransfer-btn').on('click', function(evt) {
	evt.preventDefault();
	if($('.droppable-sect').length){$('.droppable-sect').droppable( "destroy" ).removeClass('droppable-sect');}
	$(this).addClass('disabled').closest('.schedule-slot').addClass('selected').children(0).addClass('droppable-sect');
	DragDropTransfer();
    
	if(!$(this).attr('data-rel') == ''){
        var ClickTarget = '.'+$(this).attr("data-rel");
        
	$("#lt-panels > div").not(ClickTarget).hide();
			$(ClickTarget).show();
			ImgFit =$('.img-fit');
			if(ImgFit.length){initImgFit();}
			if($('.owl-carousel').length){var owl = $('.owl-carousel'); var owlInstance = owl.data('owlCarousel'); if(owlInstance != null) owlInstance.reinit();}
        $(window).animate({scrollTop:($(ClickTarget).offset().top-150)},500);
	}
	else{
		$("#lt-panels > div").fadeOut();
        $(".itinerary-lt-panel").fadeIn();
	};   
});
    
$('.add-activity').on('ifChecked', function(){
    if($('.droppable-sect').length){$('.droppable-sect').droppable( "destroy" ).removeClass('droppable-sect');}
     $(this).addClass('disabled').closest('.schedule-slot').addClass('selected').children(0).addClass('droppable-sect');
	 //$(".action-btn .btn").addClass('disabled');
    DragDropActivity();
    
     if(!$(this).attr('data-rel') == ''){
       var ClickTarget = '.'+$(this).attr("data-rel");
         
	$("#lt-panels > div").not(ClickTarget).hide();
			$(ClickTarget).show();
			ImgFit =$('.img-fit');
			if(ImgFit.length){initImgFit();}
			if($('.owl-carousel').length){var owl = $('.owl-carousel'); var owlInstance = owl.data('owlCarousel'); if(owlInstance != null) owlInstance.reinit();}
        $(window).animate({scrollTop:($(ClickTarget).offset().top-150)},500);
       //initScrollPanel();
	}
	else{
		$("#lt-panels > div").fadeOut();
        $(".itinerary-lt-panel").fadeIn();

       //initScrollPanel();
	};
});

$(document).on('click','.addactivity-btn', function(evt) {
if($('.droppable-sect').length){$('.droppable-sect').droppable( "destroy" ).removeClass('droppable-sect');}
     $(this).addClass('disabled').closest('.schedule-slot').addClass('selected').children(0).addClass('droppable-sect');
	 //$(".action-btn .btn").addClass('disabled');
    DragDropActivity();
    
     if(!$(this).attr('data-rel') == ''){
       var ClickTarget = '.'+$(this).attr("data-rel");
         
	$("#lt-panels > div").not(ClickTarget).hide();
			$(ClickTarget).show();
			ImgFit =$('.img-fit');
			if(ImgFit.length){initImgFit();}
			if($('.owl-carousel').length){var owl = $('.owl-carousel'); var owlInstance = owl.data('owlCarousel'); if(owlInstance != null) owlInstance.reinit();}
        $(window).animate({scrollTop:($(ClickTarget).offset().top-150)},500);
       //initScrollPanel();
	}
	else{
		$("#lt-panels > div").fadeOut();
        $(".itinerary-lt-panel").fadeIn();

       //initScrollPanel();
	}; 
});

$(".btn-hotel-sec").click(function(){
    $(".hotel-sec, .hotel-dsc").show();
    $(".itinerary-lt-panel, .upgrade-transfer-sec, .details-sec-cont").hide();
    $(".hotel-sec-upgraded").addClass('selected');
	$(".details-sec-cont").removeClass('selected');
   // $(".btn-hotel-sec").addClass('disabled');
});
    
$(document).on("click",".sec-toggle-edit", function(){
    $(this).closest('.schedule-slot').find(".no-of-passengers").slideToggle(); 
	$(this).toggleClass("active");
});

$(document).on("click",".close-rt", function(){
    $(this).closest('.schedule-details').find(".no-of-passengers").slideUp(); 
	$(this).toggleClass("active");
});
   
$(".schedule-slot .close-rt").click(function (e){
   // $(this).closest('.schedule-details').find(".no-of-passengers").slideUp(); 
        $(this).closest('.details-sec').slideUp(); 
	$(this).removeClass("active");
	//$(this).removeClass('disabled');
	e.preventDefault();
});

$(".add-extra-nights").click(function(){
   $(".extra-nights-details").slideToggle(); 
   $(".add-extra-nights-sec").show();
   $(".undo-redo-opt").hide();
   $(".price span").removeClass("pr-updated");
   $(".ajax-btn").addClass("disabled");
    $(".itinerary-lt-panel, .day-02-sec").hide();
	$(this).toggleClass("disabled");
});

$('#lt-panels .close-rt').click(function (e){
    $("#lt-panels > div").fadeOut();
    $(".itinerary-lt-panel").fadeIn();
	e.preventDefault();
});

$('.filter-sec .close-rt').click(function (e){
    $(".filter-sec-on-click").fadeOut();
    //$(".left-sidebar").fadeIn();
	e.preventDefault();
});

//Add meal
$("#chkMeal").click(function(){
	$("#expand-meal").slideToggle(0); 
	$("#mealAdded").toggleClass("selected");
	
});
/* Custom Trip : Ends */

/** Drag & Drop **/
var DragDropHotel = function() {
$( ".draggable-sect > li" ).draggable({
	appendTo: ".wrapper",
	helper: function() {
  var helper = $(this).clone(); 
  // jquery.ui.sortable will override width of class unless we set the style explicitly.
 //alert($(this).width()+"++++"+$(this).height());
  helper.css({'width': $(this).width(), 'height': $(this).height()});
  return helper;
},
	addClasses: false,
    cursor: 'move'
});
$( ".droppable-sect" ).droppable({
	addClasses: false,
	activeClass: "ui-state-default",
	hoverClass: "ui-state-hover",
	accept: ":not(.dragged)",
	drop: function( event, ui ) {
           $('.changehotelsec-lt-panel').find('.dragged').removeClass('dragged');
			ui.draggable.addClass('dragged');
            var HotelTitle = ui.draggable.find('.img-caption').text();
            var HotelRating = ui.draggable.find('.star-rating').attr('class');
           
          //alert(HotelTitle+"====="+HotelRating);
        
           $(this).closest('.schedule-slot').find('.iti-tl').find('b').text(HotelTitle);
            $(this).closest('.schedule-slot').find('.star-rating').attr({'class':HotelRating});
        
             var OldPrcFigure = $('.price .total-amt').text().replace(",", ""),
				AddPrcFigure = ui.draggable.find('.txt-uprgrd-btn b > span').text().replace(",", "").replace("*", "");
            //alert(OldPrcFigure);
			var NewPrcFigure=addnumbers(OldPrcFigure,AddPrcFigure);
             
            $('.total-amt').text(NewPrcFigure).digits().parent().addClass('pr-updated');
            $('.undo-redo-opt').show();

  }
});
};


var DragDropActivityTransfer = function() {
$( ".draggable-sect > li" ).draggable({
	appendTo: ".wrapper",
	helper: function() {
  var helper = $(this).clone(); 
  // jquery.ui.sortable will override width of class unless we set the style explicitly.
 //alert($(this).width()+"++++"+$(this).height());
  helper.css({'width': $(this).width(), 'height': $(this).height()});
  return helper;
},
	addClasses: false,
    cursor: 'move'
});
$( ".droppable-sect" ).droppable({
	addClasses: false,
	activeClass: "ui-state-default",
	hoverClass: "ui-state-hover",
	accept: ":not(.dragged)",
	drop: function( event, ui ) {
           
           $('.upgradetransfersec-lt-panel').find('.dragged').removeClass('dragged');
			ui.draggable.addClass('dragged');
            var TransferTitle = ui.draggable.find('.name-on-img').text();
           $(this).closest('.schedule-slot').find('.iti-tl').find('trasnfer-name').text(TransferTitle);
        
            var OldPrcFigure = $('.price .total-amt').text().replace(",", ""),
				AddPrcFigure = ui.draggable.find('.txt-uprgrd-btn b span').text().replace(",", "").replace("*", "");
			var NewPrcFigure=addnumbers(OldPrcFigure,AddPrcFigure);
        
       $(this).find('.action-btn .no-list li + li').hide();
        $(this).find('.action-btn .no-list li .btn-check span').text('Transfer Upgraded');
        
        $(this).find('.price-figures').text(AddPrcFigure).digits();
        //alert(AddPrcFigure);
             
            $.magnificPopup.open({items: {src: '#upgrade-transfer'},type: 'inline'});

            $('.total-amt').text(NewPrcFigure).digits().parent().addClass('pr-updated');
            $('.undo-redo-opt').show();
    }
});
};

var DragDropTransfer = function() {
$( ".draggable-sect > li" ).draggable({
	appendTo: ".wrapper",
	helper: function() {
  var helper = $(this).clone(); 
  // jquery.ui.sortable will override width of class unless we set the style explicitly.
 //alert($(this).width()+"++++"+$(this).height());
  helper.css({'width': $(this).width(), 'height': $(this).height()});
  return helper;
},
	addClasses: false,
    cursor: 'move'
});
$( ".droppable-sect" ).droppable({
	addClasses: false,
	activeClass: "ui-state-default",
	hoverClass: "ui-state-hover",
	accept: ":not(.dragged)",
	drop: function( event, ui ) {
           
           $('.upgradetransfersec-lt-panel').find('.dragged').removeClass('dragged');
			ui.draggable.addClass('dragged');
            var TransferTitle = ui.draggable.find('.name-on-img').text();
           $(this).closest('.schedule-slot').find('.iti-tl').find('small').text(TransferTitle);
        
            var OldPrcFigure = $('.price .total-amt').text().replace(",", ""),
				AddPrcFigure = ui.draggable.find('.txt-uprgrd-btn b span').text().replace(",", "").replace("*", "");
			var NewPrcFigure=addnumbers(OldPrcFigure,AddPrcFigure);

            $('.total-amt').text(NewPrcFigure).digits().parent().addClass('pr-updated');
            $('.undo-redo-opt').show();
    }
});
};

var DragDropActivity = function() {
$( ".draggable-sect > li" ).draggable({
	appendTo: ".wrapper",
	helper: function() {
  var helper = $(this).clone(); 
  // jquery.ui.sortable will override width of class unless we set the style explicitly.
 //alert($(this).width()+"++++"+$(this).height());
  helper.css({'width': $(this).width(), 'height': $(this).height()});
  return helper;
},
	addClasses: false,
    cursor: 'move'
});
$( ".droppable-sect" ).droppable({
	addClasses: false,
	activeClass: "ui-state-default",
	hoverClass: "ui-state-hover",
	accept: ":not(.dragged)",
	drop: function( event, ui ) {
           $('.addactivitiessec-lt-panel').find('.dragged').removeClass('dragged');
			ui.draggable.addClass('dragged');
			$(this).find('.iti-tl').find('b').text();
			var OldPrcFigure = $('.pkg-bar-list .price .total-amt').text().replace(/,/g, "").replace(",", ""),
				AddPrcFigure = ui.draggable.find('.schedule-content > b > span').text().replace(",", "").replace("*", "");
			var NewPrcFigure=addnumbers(OldPrcFigure,AddPrcFigure);
            //alert(OldPrcFigure); 
            $('.total-amt').text(NewPrcFigure).digits().parent().addClass('pr-updated');
            //alert(NewPrcFigure); 
            $('.undo-redo-opt').show();
			$( "<li class='schedule-slot selected'></li>" ).html( ui.draggable.html() ).insertAfter($(this).closest('.schedule-slot'));
            $(".customize-trip").addClass("activeSec");
  }
});
};

function addnumbers(OldPrcFigure,AddPrcFigure){
    var sumprice=Number(OldPrcFigure)+Number(AddPrcFigure);
    return sumprice;
}
/** Price Digits Number **/
$.fn.digits = function(){ 
    return this.each(function(){ 
        $(this).text( $(this).text().replace(/(\d)(?=(\d\d\d)+(?!\d))/g, "$1,") ); 
    })
}





/* Tiles Section Customization : Starts */
$(document).on("click",".tool-tip > a, .tool-tip > i",function(e) {
	e.preventDefault();    
	$('.tooltip-cont, .share-collapse').hide();
    $('.pkg-listing.list-view > li').css({'z-index':'1'});
	$(this).parents('li').css({'z-index':'2'});
	$(this).parent().find(".tooltip-cont").show();

    var containerPos = $(this).parents('.container').offset().left;
    var containerWdth = $(this).parents('.container').outerWidth();
    var ContPos = $(this).closest('.tool-tip').children('.tooltip-cont').offset().left;
    var ContWdth=$(this).closest('.tool-tip').children('.tooltip-cont').outerWidth();
    var pos=ContPos-containerPos;
    var totWdth=pos+ContWdth;
    var margin=totWdth-containerWdth+30;
    if(totWdth>containerWdth)
    { $(this).closest('.tool-tip').children('.tooltip-cont').offset({left:ContPos-margin});}
    $(".tool-tip > a, .tool-tip > i, .share-tooltip > a").removeClass("active");
    $(this).addClass("active");
});


$(document).on("click",".tooltip-cont .btn-close",function(e) { 
	$(this).parent(".tooltip-cont").hide();
    $(".tool-tip > a, .tool-tip > i").removeClass("active");
    e.preventDefault();
});
/* Tiles Section Customization : Starts */

var initScrollPanel = function() {
    $('.scrollable-panel').each(function(i, elem2) {
        
        
        var TargetScrollSect = $(this);
        var TargetPanel = $(this).find('.scroll-container');
        var MainWrapper  = $(window).innerHeight();
        //var FixedPanelHt  = $(this).siblings('.fixed-panel').height();
        
        var TargetScrollSectHt = TargetScrollSect.height();
        var TargetPanelHt = TargetPanel.height();
        var headerHt = $('header').height();
        var footerHt = $('.sticky-bar').height();

        var FixedPanelHt = 0;
        $(this).siblings('.fixed-panel').each(function() {FixedPanelHt += parseFloat($(this).height());});
        
         //alert(MainWrapper+" and "+headerHt+" and "+footerHt)
            
        if(!Modernizr.mq(mq_green)){
            
              if(!$(this).find('.webScroller').length>0){
            TargetScrollSect.append('<div class="webScroller"><div class="up-scroll">Up</div><div class="down-scroll">down</div></div>');
                  //console.log('hi 2');
            }
             /** Common Scroll Section detect **/
         
             TargetPanel.css({height:(MainWrapper-(FixedPanelHt+headerHt+footerHt))});
             /**Sub Scroll Section detect **/
            $('.sub-scroll-sect').each(function(){
                var SubScrollPanel  = $(this).find('.sub-scroll-panel');
            //var SubFixedPanelHt  = SubScrollPanel.siblings('.sub-fixed-panel').height();
            var SubScrollSect = $(this);
                
                  var SubFixedPanelHt = 0;
                SubScrollPanel.siblings('.sub-fixed-panel').each(function() {SubFixedPanelHt += parseFloat(($(this).height()));});
        
               // console.log(SubFixedPanelHt);
                
                //alert((SubScrollSect.height()) + ' and ' + (SubFixedPanelHt+FixedPanelHt));
                //alert(TargetPanel.height())
            if(SubScrollPanel.length){
                SubScrollPanel.css({height:((TargetPanel.height())-(SubFixedPanelHt+FixedPanelHt))});
                SubScrollPanel.find(TargetPanel).css({height:(SubScrollPanel.height())});
                 //console.log('hi 2 sub scroll');
            }
            })

             scrollHandler();
                    if($(TargetPanel).hasScrollBar('Yes')){
                       $(this).find(".webScroller").find("div").stop().slideDown();
                    }
                    else{
                         $(this).find(".webScroller").find("div").stop().slideUp();
                    }
            
          TargetPanel.niceScroll({cursorcolor:"rgba(0,0,0,0.4)", cursorborder:0, cursorwidth:5}).resize();
         // TargetPanel.niceScroll().scrollend(function(info){})
           
        }
        else{
           TargetPanel.css({'height':'auto', 'overflow':'auto'});
           $('.scrollable-panel').css({'height':'auto', 'overflow':'auto'});
        }
});
}

/** Scroll **/
/***************** scoll detection ***/
$(document).delegate('.up-scroll','click',function(evt) {
     $(this).closest('.scrollable-panel').find('.scroll-container').stop().animate({scrollTop: ("-=300px")}, 700, "easeOutExpo");
})

$(document).delegate('.down-scroll','click', function() {
     $(this).closest('.scrollable-panel').find('.scroll-container').stop().animate({scrollTop: ("+=300px")}, 700, "easeOutExpo")
});

function scrollHandler() {
 //var $win = $('.scrollable-panel');
 $('.scroll-container').bind('scroll',function () {
	// var thisScrollDiv = $(this).find('.scroll-container');
	 if ($(this).scrollTop() == 0)
	 {
		 $(this).parent().find(".up-scroll").stop().slideUp();
		 $(this).parent().find(".down-scroll").stop().slideDown();
	 }
	 else if ($(this).parent().height() < $(this).height() + $(this).scrollTop()) {
		 $(this).parent().find(".down-scroll").stop().slideUp();
	 }
	 else{
	 $(this).parent().find(".up-scroll").stop().slideDown();
	 $(this).parent().find(".down-scroll").stop().slideDown();
	 }
 });
};


/* Popup : Starts */
$(document).on('click','.popup-inline', function(event) {
    var targetDiv = $(this).attr("href");
    event.preventDefault();
    $.magnificPopup.open({
	type:'inline',
   items:{ src : targetDiv},
	fixedContentPos:true,
	fixedBgPos:true,
	overflowY:'auto',
	closeBtnInside:true,
	preloader:false,
	midClick:true,
	removalDelay:300,
	mainClass:'my-mfp-slide-bottom'
});
$('.toggle-checkbox').bootstrapSwitch({state: true});
});
/* Popup : Ends */

$('.popup-box .btn-close').click(function(e){
	$('.mfp-close').click();
	e.preventDefault();
});

//Horizontal Tab
var initHorizontalTab = function() {
$('#HorizontalTab, #HorizontalTab1').responsiveTabs({
	startCollapsed: 'accordion',
	collapsible: 'accordion'
});
}


//Custome tab functionality for Itinerary, Activities, Meals
$('.toggle-btn-grp').each(function(){
	var $active, $content, $links = $(this).find('a');
	$active = $($links.filter('[href="'+location.hash+'"]')[0] || $links[0]);
	$active.addClass('active');
	$content = $($active[0].hash);
	$links.not($active).each(function () {
		$(this.hash).hide();
	});
	$(this).on('click', 'a', function(e){
		e.preventDefault();
		$active.removeClass('active');
		$content.hide();
		$active = $(this);
		$content = $(this.hash);
		$active.addClass('active');
		$content.show();
		equalheight('.equal-heights > div, .equal-heights > li');
	});
});

$('.r-tabs .r-tabs-nav .r-tabs-anchor, .view-toggle-btn').click(function(e) {
	e.preventDefault();
});

$('.resp-tab-item, .resp-accordion, .view-toggle-btn,.HrzTab .resp-tabs-list li a').click(function() {
  equalheight('.equal-heights > div, .equal-heights > li');
});
//Custome tab functionality for Itinerary, Activities, Meals Ends
/*Single Item Carousel*/
var initSingleItemCarousel = function() {
  $('.single-item-carousel').owlCarousel({
   items:1,loop:false,margin:0,nav:false,mouseDrag:false,pullDrag:false, drag:false,navText:[ , ],responsive:{0:{items:1},600:{items:1},1000:{items:1}}
});  
}
/*Single Item Carousel*/

/*Three Item Carousel*/
var initTwoItemCarousel = function() {
  $('.two-item-carousel').owlCarousel({
   items:2, loop:true,margin:1,nav:false,navText:[ , ],mouseDrag: false,pullDrag: false,responsive:{0:{items:1},600:{items:2},1000:{items:2}}
});  
}
/*Three Item Carousel*/

/*Three Item Carousel*/
var initThreeItemCarousel = function() {
  $('.three-item-carousel').owlCarousel({
   items:3, loop:true,margin:10,nav:true,navText:[ , ],mouseDrag: false,pullDrag: false,responsive:{0:{items:1},600:{items:2},1000:{items:3}}
});  
}
/*Three Item Carousel*/
/*Four Item Carousel*/
var initFourItemCarousel = function() {
  $('.four-item-carousel').owlCarousel({
   items:4, loop:true,margin:1,nav:false,navText:[ , ],mouseDrag: false,pullDrag: false,responsive:{0:{items:1},600:{items:2},1000:{items:4}}
});  
}
/*Three Item Carousel*/
/*Single Item Carousel*/
var initmonthCarousel = function() {
  $('.month-carousel').owlCarousel({
   items:1,loop:false,margin:0,nav:true,dots:false,mouseDrag:false,pullDrag:false, drag:false,navText:[ , ],responsive:{0:{items:1},600:{items:1},1000:{items:1}}
});  
}
/*Single Item Carousel*/

$('.flexible-dates .owl-carousel').owlCarousel({
    items:7,loop:true, center:true,margin:0,nav:true,dots:false,mouseDrag:false,pullDrag:false, drag:false,navText:[ , ],responsive:{0:{items:1},480:{items:3},600:{items:4},1000:{items:7}}
});

$('.featured-prod .owl-carousel').owlCarousel({
    items:3,loop:true, center:true,margin:0,nav:true,dots:false,mouseDrag:false,pullDrag:false, drag:false,navText:[ , ],responsive:{0:{items:1},480:{items:3},600:{items:3},1000:{items:3}}
});

/*Holiday-Customise-Activity-Upgrade-Transfer-Entire-Trip-Popup-Selection Functionality start*/
$('.upgrade-activity').on('ifChecked', function(){
	$(".trans-list").show();
	$(".btn").removeClass("disabled");
});
/*Holiday-Customise-Activity-Upgrade-Transfer-Entire-Trip-Popup-Selection Functionality Ends*/



/*Gallery View Switcher starts*/
$('.gal-view-switcher').on('click', 'li', function(){
	$('.gal-view-switcher li').removeClass('active');
	$(this).addClass('active');
});

$( ".map" ).click(function() {
	$( ".map-panel" ).show();
	$( ".photos-panel" ).hide();
});
$( ".photos" ).click(function() {
	$( ".map-panel" ).hide();
	$( ".photos-panel" ).show( );
	if(booleanValue === true){
	  booleanValue = false;
	} else if(booleanValue === false){
	  booleanValue = true;
	}
 
	owl.data('packagedetails, sync2').reinit({
		singleItem : booleanValue
	});
});
/*Gallery View Switcher ends*/

/** Radio Button TOggle ***/
/* Map Image bootstrapSwitch :: Starts */
$('.toggle-checkbox').each(function(){
    if($(this).closest('.view-switch').find('.ttl-view').length){
        $(this).bootstrapSwitch({state: true, inverse: true});
    }
    else{
        $(this).bootstrapSwitch({state: true});
    }
});

$('.switch-state').each(function(){
	$(this).bootstrapSwitch({inverse: true});
		
	$(this).on('switchChange.bootstrapSwitch', function(event, state) {
		$(this).closest('.view-switch').find('.ttl-view').toggleClass("disabled")
		//$(this).closest('.switch-div').find('.switch').toggleClass('disabled');
	});

	$(this).closest('.view-switch').find('.ttl-view').on('click', function () {
		var type
		type = $(this).data('switch-set')
		$(this).closest('.view-switch').find('.switch-' + type).bootstrapSwitch(type, $(this).data('switch-value'));
	});
});
/* Map Image bootstrapSwitch :: Ends */

/** required Fields Focus **/
var initCustomField = function(){
	$('.custom-field').each(function(){    
        
		var $this = $(this), 
            ReqHolder = $this.find('.holder'), 
            thisText = $this.find('input, textarea,.textarea, button').val(), 
            thisfield = $this.find("input, textarea,.textarea, button");
    
		// initialisation
		if(thisText.length){ReqHolder.fadeOut();}
		else{ReqHolder.fadeIn();}
		//focus
		thisfield.focus(function(){ReqHolder.fadeOut();	}).focusout(function(){
			if($this.find('input, textarea, button').val().length){ReqHolder.fadeOut();}
			else{ReqHolder.fadeIn();}
		});
		
	});	
}
/** required Fields Focus **/

/****** Search Result starts ******/

$(document).on('click', '.pkg-similar .pkg-similar-head', function (){
  // var TargetExpandHeight = $(this).closest('.pkg-similar').find('.pkg-similar-list').height();
     if(!$(this).hasClass('active')){
         $(this).addClass("active");
         //$(TargetExpandParent).css({'margin-bottom':'+='+TargetExpandHeight});
         $(this).parent().find(".pkg-similar-list").slideDown();
         //$(this).closest('.ajax-entry-rec').animate({'margin-bottom':TargetExpandHeight},500);
     }
         else{
             //$(this).closest('.ajax-entry-rec').animate({'margin-bottom':0},500);
             $(this).parent().find(".pkg-similar-list").slideUp();
             $(this).removeClass("active");
         }
});


/* 10 July 2017
$('#recommend-pkg-tabs > li').hover(function(){
    //var indexCount = $('#recommend-pkg-tabs > li').index( this );
    var TargetDetails= $(this).find('.on-hover-details');
    $(".pkg-similar-list").hide()
    $(".pkg-similar-head").removeClass('active');
    $(TargetDetails).closest('ul').css({'height':'auto'});
    var TargetHeight= ($(this).find('.on-hover-details').height()+1);
    
    if(!$(this).hasClass('active')){
        $('#recommend-pkg-tabs > li').removeClass('active');
        $(this).addClass('active'); 
        $(".on-hover-details").not(TargetDetails).removeClass('active').stop().slideUp();
        $(this).parent().stop().animate({'margin-bottom':TargetHeight},500, function(){
            TargetDetails.css({'opacity':1}).stop().fadeIn().addClass('active');
        });
    //})
    }else{}
    
},function(){
   var TargetDetails= $(this).find('.on-hover-details');
    $(".pkg-similar-list").hide();
     $(".pkg-similar-head").removeClass('active');
    $(TargetDetails).closest('ul').css({'height':'auto'});
    
   $(".on-hover-details").removeClass('active').stop().slideUp();
   $('#recommend-pkg-tabs > li').removeClass('active');
   $('#recommend-pkg-tabs').stop().animate({'margin-bottom':0});

} );
*/

/****** Search Result ends ******/

/******* Details page starts *****/

/*Map popup functionality starts*/
$('.map-panel > img').click(function(){
	$(this).parent().find('.map-hotel-popup').toggle();
});
/*Map popup functionality ends*/

/******* Details page Ends *****/

$(".menuTrigger").click(function(){
    $(".floating-btns > ul").slideToggle();
    $(this).toggleClass("active");
});

/* init hmoe tabs :Starts */
var initbannerImgFit = function() {
	$(bannerImgFit).find("img").each(function(i, elem) {
		var img = $(elem);
		var imgClass = $(elem).attr("class");
		var imgAlt = $(elem).attr("alt");
		var div = $("<div />").css({
			background:"url(" + img.attr("src") + ") no-repeat",
			'background-size':"cover",
			width:"100%",
			height:"100%"
		}).addClass(imgClass).attr("title",imgAlt);
		img.replaceWith(div);
	});
}
/* init hmoe tabs :Ends */

/*disable href="tel:" in desktop*/

if (navigator.userAgent.match(/(iPhone|Android|BlackBerry)/)) {
//this is the phone

} else if (navigator.userAgent.match(/(iPod|iPad)/)) {
    $('a[href^=tel]').click(function(e){
      
    });
} else {
 //this is the browser

    $('a[href^=tel]').click(function(e){
        e.preventDefault();
    });
    $('a[href^=tel]').addClass('tel-disabled')
}

/*disable href="tel:" in desktop*/

    
$(".btn-hotel-sec").click(function(){
    $(".hotel-sec, .hotel-dsc").show();
    $(".itinerary-lt-panel, .upgrade-transfer-sec, .details-sec-cont").hide();
    $(".hotel-sec-upgraded").addClass('selected');
	$(".details-sec-cont").removeClass('selected');
   // $(".btn-hotel-sec").addClass('disabled');
});


/*SWITCHER JS - Starts*/
$("a.viewswitcher").bind("click", function(e){
    e.preventDefault();
    var theid = $(this).attr("id");
    var theproducts = $("#pkg-listings");
    var classNames = $(this).attr('class').split(' ');

    if($('.bus').length)
        {
            $('.travel-detail-container').slideUp();
            $('.result-item').css("height","auto");
            $('.bus .head-pageinfo').show();

        }

    if($(this).hasClass("active")) {
        return false;
    } else {
        if(theid == "listview") {

            $(this).addClass("active");
            $("#gridview").removeClass("active");
            $("#mapview").removeClass("active");
            theproducts.removeClass("grid-view");
            theproducts.addClass("list-view");
            $(".recommended-holidays, .pkg-list, .top-filtersec .view-switch").show();
            $(".map-view-cntrnr").hide();
             if($('.bus').length)
        {
   
        $('.bus .lht-sec').height('auto');
        $('.bus .rht-sec').height('auto');
 
        }
             
        }

        else if(theid == "gridview") {
            $(this).addClass("active");
            $("#listview").removeClass("active");
            $("#mapview").removeClass("active");
            // remove the grid view and change to list
            $(".recommended-holidays, .pkg-list, .top-filtersec .view-switch").show();
            $(".map-view-cntrnr").hide();
            theproducts.addClass("grid-view");
            theproducts.removeClass("list-view");
            //equalheight('.equal-heights > div, .equal-heights > li, .list-view .pkg-thumbnail > div, .list-view .pkg-thumbnail > div > div > div');

        if($('.bus').length)
        {
            $('.bus .head-pageinfo').hide();
        
        var highestResultBox = 0;
        var highestResultBoxRh = 0;
        $('.bus .result-item .lht-sec').each(function()
            {  
            if($(this).height() > highestResultBox){  
                highestResultBox = $(this).height();  
            }
        });   
        $('.bus .result-item .rht-sec').each(function()
            {  
            if($(this).height() > highestResultBoxRh){  
                highestResultBoxRh = $(this).height();  
            }
        }); 
        $('.bus .result-item .lht-sec').height(highestResultBox);
        $('.bus .result-item .rht-sec').height(highestResultBoxRh);
 
        }
            
        } 

        else if(theid == "mapview") {
            $(this).addClass("active");
            $("#listview").removeClass("active");
            $("#gridview").removeClass("active");
            $(".sort-sec, .recommended-holidays, .pkg-list, .top-filtersec .view-switch").hide();
            $(".map-view-cntrnr").show();
            // remove the grid view and change to list
            theproducts.addClass("grid-view");
            theproducts.removeClass("list-view");
            //equalheight('.equal-heights > div, .equal-heights > li, .list-view .pkg-thumbnail > div, .list-view .pkg-thumbnail > div > div > div');
        } 
    }
});
/*SWITCHER JS - Ends*/

/*Flight Result - Starts*/
$("#listview-flight").click(function(){
    $("#listview-details").show();
	$(".viewswitcher").removeClass('active');
    $("#calendar-view-details, #grid-view-details").hide();
});
$("#calenderview-flight").click(function(){
    $("#calendar-view-details").show();
    $("#listview-details, #grid-view-details").hide();
	$(".viewswitcher").removeClass('active');
});
$("#gridview-flight").click(function(){
    $("#grid-view-details").show();
    $("#listview-details, #calendar-view-details").hide();
	$(".viewswitcher").removeClass('active');
});
/*Flight Result - Ends*/
/*Customize trip popup - Starts*/


// ======== Customize Trip Modal start ======== //
$("#btnCustomizeTrip").click(function(e){
    e.preventDefault();
    if(!$(this).hasClass('active')){
        $(this).addClass("active");        
        $(".modal-type01").hide();    
        $("#modalCustomizeTrip").slideDown(); 
    }
});
// ======== Customize Trip Modal End ======== //

// ======== Like it book it Modal start ======== //
$("#btnBookNow").click(function(e){
    e.preventDefault();
    if(!$(this).hasClass('active')){
        $(this).addClass("active");        
        $(".modal-type01").hide();    
        $("#modalBookNow").slideDown(); 
    }
});
// ======== Like it book it Modal End ======== //

// ======== Like it book it Modal start ======== //
$(".trip-edit").click(function(e){
    e.preventDefault();
    if(!$(this).hasClass('active')){
        $(this).addClass("active");        
        $(".modal-type01").hide();    
        $("#modalEditDetails").slideDown(); 
    }
});
// ======== Like it book it Modal End ======== //



$(".modal-type01 .button-close").click(function(e){
    $(".modal-type01").slideUp();  
    e.preventDefault();    
    $(this).parents(".right-section").find(".btn").removeClass("active");
    $(".trip-edit").removeClass("active");
});


//
//$(".btn-customize-trip").click(function(){
//if(!$(this).hasClass('active')){
//    /*23Aug20178*/ 
//    $(".trip-edit-popup").hide(); 
//    $(".modal-type01").hide();
//    $(this).siblings(".modal-type01").slideDown();
//    $(this).siblings(".modal-type01").addClass("customize-popup");
//    $(this).siblings(".modal-type01").removeClass("booknow-popup");
//    $(".trip-edit").removeClass("active");
//    /*23Aug20178*/
//    $(".likeit-bookit").hide();
//    $(".continue").show(); 
//}else{
//    $(this).siblings(".modal-type01").slideUp();    
//}
//});
//
//$("#btn-booknow").click(function(e){
//    $(".modal-type01").hide();
//    $(this).siblings(".modal-type01").slideDown();
//    $(".modal-type01").addClass("booknow-popup");
//    $(".modal-type01").removeClass("customize-popup");
//    $(".trip-edit-popup").slideUp();
//    $(".trip-edit").removeClass("active");
//    e.preventDefault();
//});

//$(".btn-Likeit-Bookit").click(function(){
//if(!$(this).hasClass('active')){
//    $(".modal-type01").slideDown();
//    $(".likeit-bookit").show();
//    $(".continue").hide();
//}else{
//    $(".modal-type-01").slideUp(); 
//}
//});

/*Activities Starts*/

//$(".btn-booknow").click(function(){
//    $(this).toggleClass("active");
//   $(".modal-type02").slideDown(); 
//});

/*Activities Ends*/

/** Cars Starts */
    $(".customize").click(function(){
    $(this).toggleClass("added2");  
    $(this).parents('.booknow-sec-right').find(".modal-type01").slideDown(); 
        return false;
});
    
     $(".cars.within-city .modal-type02.large .modal-container .button-close").click(function(){
     $('.customize').removeClass("added2");  
    $('.btn-book-now1').removeClass("added2");
});
    
    $(".btn-book-now1").click(function(){
    $(this).toggleClass("added2");  
    $(this).parents('.booknow-sec-left').find(".modal-type01").slideDown();
        return false;
});
/*Cars Ends*/

$(".trip-edit").click(function(){
    $(".right-section .modal-type01").slideUp();
    $(".trip-edit-popup .modal-type01").slideDown();
   $(".trip-edit-popup").slideDown();
    $(this).addClass("active");
});

$(".trip-edit-popup .button-close").click(function(){
    $(this).parents('.trip-edit-wrap').find(".trip-edit").removeClass("active");
});

$('.select-opt').change(function(){
if($('.select-opt').val() == 'double-with-one-child'){
$(".double-with-one-child").show();
}
else{
$(".double-with-one-child").hide();
}
});
     
$('.room-spinner .count-up').click(function(){
if($('.room-spinner .form-control').val() == '1'){
$(".add-room-two").show();
}
});

$('.room-spinner .count-down').click(function(){
if($('.room-spinner .form-control').val() == '2'){
$(".add-room-two").hide();
}
});


/*Customize trip popup - Ends*/
/*Send Enquiry popup - Starts*/
$(".snd-enq-btn").click(function(e){
    e.preventDefault();
    $(".send-enquiry-popup").slideToggle();
});
$(".submit-query").click(function(){
   $(".send-enquiry-popup-cont").hide(); 
$(".enq-submitted").show();    
});
$(".send-enquiry .btn-close").click(function(){
    $(".send-enquiry-popup").hide();
})
/*Send Enquiry popup - Ends*/
/*Tab List Carousel :: Starts
function TabListCarousel(){

if($(".tablist-carousel.owl-carousel").length){
    
var fixOwl = function(){
    //callBack();
      $('.sub-scroll-panel').each(function(){
        var $stage = $(this).find('.owl-stage'),
            stageW = $stage.width(),
            $el = $(this).find('.owl-item'),
            elW = 0;
        $el.each(function() {
            elW += ($(this).width()+10)+ +($(this).css("margin-right").slice(0, -2))
        });
       
          console.log(elW+" and " + stageW);
          if ( elW > stageW ) {
            $stage.width( elW );
              $(this).find('.owl-prev').addClass('disabled');
              // console.log(elW+" and " + stageW);
        }
                else{
                    $(this).find('.owl-next').addClass('disabled');
                  $(this).find('.owl-prev').addClass('disabled');
                    
                }          
            })
    };
    

     var $tab_Count =  $(".tablist-carousel.owl-carousel"),
    wysiwyg_owloptions = {margin:10,autoWidth:true, items:  7, loop:false, nav:true, mouseDrag: false, pullDrag: false,navText:[ , ], pullDrag: false,  dots:false, onInitialized: fixOwl,responsive:{1200:{items:7},1024:{items:10,autoWidth:true},768:{items:10,autoWidth:true,},600:{items:5,autoWidth:true},480:{items:6,autoWidth:true,},320:{items:4,autoWidth:true}}   }

    $tab_Count.each(function(){
        $(this).owlCarousel(wysiwyg_owloptions);
        //$(this).find('.owl-prev').show();
        $(this).on('initialized.owl.carousel changed.owl.carousel', function (event) {
    if (!event.namespace) return;
    var carousel = event.relatedTarget,
        element = event.target,
        current = carousel.current();
    $(this).find('.owl-next', element).toggleClass('disabled', current === carousel.maximum());
    $(this).find('.owl-prev', element).toggleClass('disabled', current === carousel.minimum());
})
    })
    
}
  
}
/*Tab List Carousel :: Ends*/

/*** Offer Ribbon starts ****/
var initPkgThumbnail = function() {
	$('.offer-ribbon').each(function(){
		TopSpacing = $(this).outerHeight();
		TargetElem = $(this).parent().find('.img-sec');
        TargetLogo = $(this).parents('.list-view > li').find('.operator-logo');
		TargetElemTourType = $(this).parents('.list-view > li').find('.tour-type'); // Car - 24-01-17
		//TargetElem .css({top:TopSpacing});
		//TargetElemTourType .css({top:TopSpacing});
       // TargetLogo .css({top:TopSpacing});
        
	});
}
/*** Offer Ribbon Ends ****/

/*Range Slider :: Starts*/
$(".rangeSlider").slider({tooltip: 'hide',min: 3000, max: 32000, value: [3000, 32000]});
$(".rangeSlider").on("slide", function(slideEvt) {
	$(".range-1-slider").text(slideEvt.value[0]);
	$(".range-2-slider").text(slideEvt.value[1]);
});  
/*Range Slider :: Ends*/

/*Length Range Slider :: Starts*/
$(".lengthRange").slider({tooltip: 'hide',min: 15, max: 480, value: [15, 480]});
$(".lengthRange").on("slide", function(slideEvt) {
	$(".lengthFrom").text(slideEvt.value[0]);
	$(".lengthTo").text(slideEvt.value[1]);
});  
/*Length Range Slider :: Ends*/

/*Length Range Slider :: Starts*/
$(".timingRange").slider({tooltip: 'hide',min: 15, max: 480, value: [15, 32000]});
$(".timingRange").on("slide", function(slideEvt) {
	$(".timingFrom").text(slideEvt.value[0]);
	$(".timingTo").text(slideEvt.value[1]);
});  
/*Length Range Slider :: Ends*/
/*Duration Slider :: Sratrs*/
$(".DurationSlider").slider({
    tooltip: 'always',
    formatter: function(value) {
        return  value[0]+'N - ' + value[1]+'N';
    }
});
/*Duration Slider :: Ends*/
/*Duration Slider :: Starts*/
$(".DurationLengthSlider").slider({
    tooltip: 'always',
    formatter: function(value) {
        return  value[0]+'Min - ' + value[1]+'Hrs';
    }
});
/*Duration Slider :: Ends*/

/*Duration Slider :: Starts*/
$(".DurationTimeSlider").slider({
    tooltip: 'always',
    formatter: function(value) {
        return  value[0]+'AM - ' + value[1]+'PM';
    }
});
/*Duration Slider :: Ends*/

/*Price Slider :: Starts*/
$(".priceSlider").slider({tooltip: 'hide',min: 400, max: 10000, value: [400, 10000]});
$(".priceSlider").on("slide", function(slideEvt) {
	$(".price-1-slider").text(slideEvt.value[0]);
	$(".price-2-slider").text(slideEvt.value[1]);
});
/*Price Slider :: Ends*/
/*Duration Slider Day :: Sratrs*/
$(".DurationSliderDay").slider({
    tooltip: 'always',
    formatter: function(value) {
        return  value[0]+'D - ' + value[1]+'D';
    }
});
/*Duration Slider :: Ends*/

/*Filter Toggle :: Starts*/
$(".fliter-sec > form .filter-pannel .filter-inner-list").hide();
$(".fliter-sec-head .fliter-sec-toggle").click(function(){
    $(this).toggleClass("active");
    $(".fliter-sec > form .filter-pannel .filter-inner-list").slideToggle();
     $('.fliter-sec-head .fliter-sec-toggle').text(function(_, text) {
        return text === 'Show all Filters' ? 'Hide all Filters' : 'Show all Filters';
    });
     $(".filter-pannel").toggleClass("active");
    
});

/*Filter Toggle :: Ends*/

$('.form-des-element').click(function(e){
    e.preventDefault();
    $(this).closest('ul').find('.form-des-details').hide();
    $(this).closest('li').find('.form-des-details').show();
    $(this).toggleClass("active")
    /*$(this).closest('li').find(".InputCustomDatepicker").addClass("active");*/
}); 
$(document).mouseup(function (e)
{
    var container = $(".form-des-details");    
    if (!container.is(e.target) 
        && container.has(e.target).length === 0) 
    {		
        container.hide();
        $(".InputCustomDatepicker").removeClass("active");
        $(".form-des-element").removeClass("active")
    }
});

$('.month-selector ul li').click(function(e)
 {
e.preventDefault();
 $('.month-selector ul li').removeClass("selected");
 $(this).addClass("selected");
})


/* Filter Section Custom popup Start*/

$('.filter-sec .fl-close').click(function (e){
    $(".filter-overlay").fadeOut();
	e.preventDefault();
});

/* Filter Section Custom popup End*/



/*** Need help starts**
$('.btn-needhelp, .need-help .btn-close').click(function(){
	var hidden = $('.need-help');
	var NeedBtnClose = $('.need-help .btn-close');
    var ElemWidth = hidden.width()+300;
	if (hidden.hasClass('active')){
		//hidden.animate({"left":"-892px"}, "slow").removeClass('active');
        NeedBtnClose.animate({'right':'0'})
		hidden.removeClass('active').animate({'right':(-ElemWidth)});
	} else {
		//hidden.animate({"left":"0px"}, "slow").addClass('active');
		hidden.addClass('active').animate({'right':'0'}, function(){
                                            NeedBtnClose.animate({'right':'100%'})
                                             })
	}
});
/*** Need help ends***/

$( ".modify-box .btn-close" ).click(function() {
	$(this).parent(".modify-box").slideUp(); 
	$( ".modify-search" ).removeClass("active");  
});

$(".chk-availabilty .btn-close").click(function(){
    $(".chk-availabilty").hide();
    $(".check-avail").removeClass('active');
});



// Hotel Map Funcionality
$(".map-panel img").click(function(){                  
    $(".bus .cust-pop.popup-sec .sm-popup").toggleClass("active");
});

$("#select-btn").click(function(e){                
    $(".slct-brdng-pnt").show();
    $(".bording-point").hide();     
    $('.mfp-close').trigger('click');
});

$('.map-hotel-popup .btn-close').click(function(){                
    $(".bus .cust-pop.popup-sec .sm-popup").removeClass("active");
});
//Tooltip Details starts


/*** Colapsable block starts ***/
$(".collapse-link").click(function(e){
    e.preventDefault();
    $(this).toggleClass('collapsed ');
    $(this).closest('.collapse-block').children('.collapse-container').slideToggle();
    $(this).find('.link-cont').toggle();
});
/*** Colapsable block Ends ***/

$(document).on('click','#btn-continue', function(evt) {
	$.magnificPopup.close();
});


/** Tooltip starts ***/
$('[data-toggle="tooltip"]').tooltip();
/** Tooltip Ends ***/

/** Compare holidays accordins starts ***/
$(".comp-accordian .dwn-accd-arrw").click(function(e) {
	$(this).parent().next(".comp-tbl-res").slideToggle("slow");
	$(this).toggleClass("active");
	initScrollPanel();
});
/** Compare holidays accordins Ends ***/

/** shopping cart ***/
$(document).ready(function(){
ww = document.body.clientWidth;
if (ww >= 990){
$(".filter-pannel .filter-tl").click(function(){
   $(".filter-inner-list").slideToggle(); 
    $(".filter-pannel").toggleClass("active");
    $('.fliter-sec-head .fliter-sec-toggle').text(function(_, text) {
        return text === 'Show all Filters' ? 'Hide all Filters' : 'Show all Filters';
    });
}); 
}
else{
	$(".fliter-sec form").slideUp();
	$(".filter-mob-toggle").removeClass('active');
     $(".filter-pannel .filter-tl").click(function(){
        $(this).closest(".filter-pannel > ul > li").find(".filter-inner-list").slideToggle();
         $(".filter-pannel").toggleClass("active");
     });
}
});

$(".filter-mob-toggle").click(function(){
	var ww1 = document.body.clientWidth;
	
	if (ww1 < 990){
	$(this).toggleClass('active');
	$(".fliter-sec form").slideToggle();
	}
	else{}
});




$(window).bind('resize orientationchange', function() {
ww = document.body.clientWidth;
if (ww >= 990){
	$(".fliter-sec form").slideDown();
	$(".filter-mob-toggle").addClass('active');
} else{
	$(".fliter-sec form").slideUp();
	$(".filter-mob-toggle").removeClass('active');}

});

/*$(window).bind('load resize orientationchange', function() {
ww = document.body.clientWidth;
if (ww <= 767){
    alert("found");

} else{}

});*/

/*	$('.filter-search-panel-toggle.mob-display').click(function() {
	$(this).toggleClass('active');
    $('.filter-search-panel').slideToggle();
});*/


/*** Owl Carousals **/
var initOwlCarousels = function() {
    var OwlElement = $('.owl-carousel');
    var owlWrap = $('.photos-panel');
    // checking if the dom element exists
        // check if plugin is loaded
        if (jQuery().owlCarousel) {
            OwlElement.each(function(){
                // PhotoPanel carousel
                if($(this).parent().hasClass('photos-panel')){
                    var slideCount;
                    if($(this).parent().hasClass('thumb-seven')){ slideCount = 7}
                    else {slideCount = 5}
                    
                    
              owlWrap.each(function(){
                var sync1= $(this).find('.photo-gallery'),
                    sync2 = $(this).find(".photo-gallery-thumb"),
                    status = $(this).find('.owl-status'),
                    imgSlider = $(this).find('.owl-carousel'),
                    slidesPerPage = slideCount,
                    syncedSecondary = true,
                    targetOwlItem = status.find('.owlItems'),
                    targetCurrentItem = status.find('.currentItem'),
                that = $(this);
               
            if($(this).hasClass('owl-singleItem')){
               $(imgSlider).owlCarousel({
                 items : 1,
    slideSpeed : 2000,
    nav:true,
    autoplay: false,
    dots: false,
    loop: false,
    responsiveRefreshRate : 200,
    onInitialized  : function(elem){
        var CurrentCount = elem.item.count-1;
        $(targetCurrentItem) .find(".result").text(CurrentCount);
                  }
              }).on('changed.owl.carousel', syncPositionSingle);
                 
            }
           
            else{
              sync1.owlCarousel({
                 items : 1,  slideSpeed : 2000, nav: true,  autoplay: false,
    dots: false,
    loop: true,
    responsiveRefreshRate : 200,
                  onInitialized  : function(elem){
                      var CurrentCount = elem.item.count-1;
                      $(targetCurrentItem) .find(".result").text(CurrentCount);
                  }
              }).on('changed.owl.carousel', syncPosition);
            }
               
               sync2.on('initialized.owl.carousel', function () {
      sync2.find(".owl-item").eq(0).addClass("current");
    })
    .owlCarousel({
    items : slidesPerPage,
    dots: false,
    nav: false,
    smartSpeed: 200,
    loop:false,
    slideSpeed : 500,
    slideBy: slidesPerPage, //alternatively you can slide by 1, this way the active slide will stick to the first item in the second carousel
    responsiveRefreshRate : 100,
    navText: ['<svg width="100%" height="100%" viewBox="0 0 11 20"><path style="fill:none;stroke-width: 1px;stroke: #000;" d="M9.554,1.001l-8.607,8.607l8.607,8.606"/></svg>','<svg width="100%" height="100%" viewBox="0 0 11 20" version="1.1"><path style="fill:none;stroke-width: 1px;stroke: #000;" d="M1.054,18.214l8.606,-8.606l-8.606,-8.607"/></svg>']
                }).on('changed.owl.carousel', syncPosition2);;


    function syncPosition(el) {
        //alert('hi');
    //if you set loop to false, you have to restore this next line
    //var current = el.item.index;
    //if you disable loop you have to comment this block
    var count = el.item.count-1;
    var current = Math.round(el.item.index - (el.item.count/2) - .5);
    var RemainingCount = (count-current)
    
    if(RemainingCount>=0){
         $(targetCurrentItem).find(".result").text(RemainingCount);
    }
    else{
        //alert("Minus");
        $(targetCurrentItem).find(".result").text(count);
    }
        
    if(current < 0) {
      current = count;
    }
    if(current > count) {
      current = 0;
    }
    
    //end block

    sync2
      .find(".owl-item")
      .removeClass("current")
      .eq(current)
      .addClass("current");
    var onscreen = sync2.find('.owl-item.active').length - 1;
    var start = sync2.find('.owl-item.active').first().index();
    var end = sync2.find('.owl-item.active').last().index();
    
    if (current > end) {
      sync2.data('owl.carousel').to(current, 100, true);
    }
    if (current < start) {
      sync2.data('owl.carousel').to(current - onscreen, 100, true);
    }
  }
  
  function syncPosition2(el) {
    if(syncedSecondary) {
      var number = el.item.index;
      sync1.data('owl.carousel').to(number, 100, true);
    }
  }
  
  sync2.on("click", ".owl-item", function(e){
    e.preventDefault();
    var number = $(this).index();
    sync1.data('owl.carousel').to(number, 300, true);
  });
                
function syncPositionSingle(el) {
    //if you set loop to false, you have to restore this next line
    //var current = el.item.index;
    //if you disable loop you have to comment this block
    var count = el.item.count-1;
    var current = Math.round(el.item.index - (el.item.count/2) + 1);
    var RemainingCount = (count-current)

    $(targetCurrentItem).find(".result").text(RemainingCount);
  
    if(current < 0) {
      current = count;
    }
    if(current > count) {
      current = 0;
    }

  }
              })
                }
                
                // Regular carousel
                else{
                    
                    /*if($(this).hasClass('tablist-carousel')){
                        $(this).owlCarousel({autoWidth: true,  slideSpeed : 2000, nav: true,  autoplay: false,dots: false,loop: false,responsiveRefreshRate : 200})
                    }*/
                    
                    if($(this).hasClass('tablist-carousel')){
                        $(this).owlCarousel({autoWidth: true,  margin:5,navText:[ , ],slideSpeed : 2000,nav: true, autoplay: false, dots: false, loop: false, responsiveRefreshRate : 200})
                    }
                    
                    if($(this).hasClass('testimonials-items')){
                        $(this).owlCarousel({items : 1, slideSpeed : 2000, nav: false, autoplay: false, dots: true, loop: false,    responsiveRefreshRate : 200})
                    }
                    else{
                            $(this).owlCarousel({ items : 1,  slideSpeed : 2000, nav: true,  autoplay: false,dots: false,loop: true, responsiveRefreshRate : 200});
                }
                    

                
                }

                
            })
        }
}

 
/* Road Bloack Popup */
idleTimer = null;
idleState = false;
idleWait = 30000;

(function ($) {

    $(document).ready(function () {
        $('*').bind('mousemove keydown scroll', function () {
            clearTimeout(idleTimer);
            if (idleState == true) { 
                // Reactivated event
                //$("body").append("<p>Welcome Back.</p>");            
            }
            
            idleState = false;
            idleTimer = setTimeout(function () { 
                // Idle Event
                $.magnificPopup.open({
                    items: {src: '#roadblock'},
                    type: 'inline', 
                    fixedContentPos: true, 
                    fixedBgPos: true, 
                    overflowY: 'auto', 
                    closeBtnInside: true, 
                    preloader: false, 
                    midClick: true, 
                    removalDelay: 100, 
                    mainClass: 'my-mfp-slide-bottom'
                });
                //$("body").append("<p>You've been idle for " + idleWait/1000 + " seconds.</p>");
                idleState = true; }, idleWait);
        });
        
        $("body").trigger("mousemove");
    });
}) (jQuery);

/* Currency :Starts */
$('.sub-menu.currency li a').on('click',function(){
    $('.sub-menu.currency li a').removeClass('fwtb');
    $('.sub-menu.currency li a .curTick').removeClass('fa fa-check');
    $(this).addClass('fwtb');
    $(this).find('.curTick').addClass('fa fa-check');
    var ccode = $(this).attr('data-contryname');
    var currencycode = $(this).attr('data-contrycurrency');
    //var menuhtml="";
    menuhtml = ccode;
    $('.currency-main-menu').html(menuhtml);
});
/* Currency :Ends */

/*Filter Selection boxed list starts */
$('.fltr-slt-list > li > a').click(function(e)
                                 {
    e.preventDefault();
    $(this).parents('.fltr-slt-list').find('a').removeClass('selected');
    $(this).parents('.fltr-slt-list').find('li').removeClass('selected');
    $(this).addClass('selected');
})
/*Filter Selection boxed list ends */

/* Activity Search */
$('#ActivityTags').keyup(function(){
    var input = $(this).val();
    if(input == 'New Delhi'){
        $('#CityBasedSearch').show();
         $('#ActivityBasedSearch, #CityActivityBasedSearch').hide();
    }
    else if(input == 'River Rafting'){
         $('#ActivityBasedSearch').show();
         $('#CityActivityBasedSearch, #CityBasedSearch').hide();
    } 
    else if(input == 'Taj Mahal in Agra'){
         $('#CityActivityBasedSearch').show();
         $('#ActivityBasedSearch, #CityBasedSearch').hide();
         
    }else {
        $('#CityBasedSearch, #ActivityBasedSearch, #CityActivityBasedSearch').hide();
    }
});
/* Activity Search */


/***** Itinerary Sub  tabs starts *****/
var initSubItinerary = function() {
    
var fixednavbar   = $('.itenary-details'),
DivoOffset     = fixednavbar.offset().top-$('header').outerHeight();
        
 /*23Aug2017*/
/*$(window).scroll(function() {
if ($(window).width() > 767) {

        if ($(window).scrollTop()>DivoOffset)
                      {
                        $('.itenary-details .itenary-heading').addClass('fixedPos');
                        $('.itenary-details .itenary-section').addClass('itnr-sec-gap');

                      }
       else
                      {
                        $('.itenary-details .itenary-heading').removeClass('fixedPos');
                        $('.itenary-details .itenary-section').removeClass('itnr-sec-gap');
                      }
} 
    });


$(window).resize(function() {
    if ($(window).width() < 767) {
	$('.itenary-details .itenary-heading').removeClass('fixedPos'); 
	$('.itenary-heading-btn a').addClass('fixedPos'); 
    $('.itenary-details .itenary-heading').addClass('mobile');
         
    
    } else {
        $('.itenary-details .itenary-heading').removeClass('mobile');
         $('.itenary-section').show();
         $('.itenary-details .itenary-heading').slideDown();
        

    }
}).resize();*/
    /*23Aug2017*/

 $( ".itenary-heading > li a" ).click(function(event) {
	if($(".itenary-details .itenary-heading.mobile").length)
	{
		    event.preventDefault();
            $('.itenary-details .itenary-heading li a').removeClass('active');
            $(this).addClass( "active" );

            var $TargetAside = $('.itenary-details'), 
            $BtnAside = $TargetAside.find('.itenary-heading-btn'),
            $ContAside = $TargetAside.find('.itenary-heading'),
            $BtnActiveAside = $ContAside.find('.active').text();
            $BtnActiveAside = $BtnActiveAside;

            $BtnAside.find('a').text($BtnActiveAside);
            var dataLabel = $(this).attr('data-label');
            $('.itenary-section').hide()
            $("#tab-"+dataLabel+"-sect").show()
            $('.itenary-details .itenary-heading').slideUp();
	}
	else
	{
        $(this).parents('.itenary-heading').find('li a').removeClass("active");
        $(this).addClass("active");
        var dataLabel = $(this).attr('data-label');
                    var itneraryFixPos =$('.itenary-details .itenary-heading.fixedPos');
                    if(itneraryFixPos.length)
                    $('html, body').animate({ scrollTop: $("#tab-"+dataLabel+"-sect").offset().top-190 }, 300);
                    else
                    $('html, body').animate({ scrollTop: $("#tab-"+dataLabel+"-sect").offset().top-220 }, 300);
	}
 
});
    
    
  $('.itenary-heading-btn a').click(function( event ) {
            event.preventDefault();
      if($(".itenary-details .itenary-heading.mobile").length)
          {
              
            $('.itenary-details .itenary-heading').slideToggle();
            $( this ).toggleClass( "active" );
          }
             });   
}
/***** Itinerary Sub  tabs Ends *****/

/* 04/04/17 - Script for Listing Grid View Hover Effect */ 
$('#pkg-listings > li figure  a, .pkg-thumbnail figure  a').click(function(evt){evt.preventDefault()});

$(function() {

$(window).on('resize', function() {
    //alert('hi');
    
    if($('#pkg-listings').length){
    $('.openEntry').remove();
    $('.ajax-entry').hide();

    var startPosX = $('#pkg-listings > li:first').position().left;
    //console.log(startPosX);
    $('.ajax-entry, #pkg-listings > li').removeClass("first last");
    $('.ajax-entry').each(function() {
        if ($(this).prev('li').position().left == startPosX) {
            $(this).prev('li').addClass("first");
            $(this).prevAll('.ajax-entry:first').addClass("last");
        }
    });
    $('.ajax-entry:last').addClass("last");
    }
    
    if($('#recommend-pkg-tabs').length){
    $('.openEntryRec').remove();
    $('.ajax-entry-rec').hide();

    var startPosXRec= $('#recommend-pkg-tabs > li:first').position().left;
    $('.ajax-entry-rec, #recommend-pkg-tabs > li').removeClass("first last");
    $('.ajax-entry-rec').each(function() {
        if ($(this).prev('li').position().left == startPosXRec) {
            $(this).prev('li').addClass("first");
            $(this).prevAll('.ajax-entry-rec:first').addClass("last");
        }
    });
    $('.ajax-entry-rec:last').addClass("last");
    }

});

 $('#pkg-listings > li').each(function(){
    //var $thisData = $(this).html();
    var $thisClass = $(this).attr('class');
    
    if($thisClass.match(/[\w-]*group[\w-]*/g)){
        $("<li class='ajax-entry list-view col-md-12 group'></li>").insertAfter(this);
    }
    else if($thisClass.match(/[\w-]*individual[\w-]*/g)){
    $("<li class='ajax-entry list-view col-md-12 individual'></li>").insertAfter(this);
    }
    else{
    $("<li class='ajax-entry list-view col-md-12'><div class='on-hover-details1'></div></li>").insertAfter(this);
    }
        
    $(this).click(function() {
        if($('#pkg-listings').hasClass('grid-view')){
        if(!$(this).hasClass('active')){
            $(window).trigger('resize');
            $('.openEntry').slideUp();
            $('.hovered').removeClass('hovered active');
               $(this).addClass('hovered active');
            
        var preview = $(this);
        var previewData = $(this).html();
        if(!$('.openEntry').length){
			//initForms();
            preview.next('.ajax-entry').clone().addClass('openEntry').html(previewData).insertAfter(preview.nextAll('.last:first')).wrapInner( "<div class='on-hover-details1'></div>" ).height('auto').stop().css({'margin-top':-1}).slideDown();
            $('.on-hover-details1').find('.iCheck-helper, .check').remove();
            $('.openEntry').find('input[type=radio]').unwrap('.styled-radio')
			
            $.scrollTo($(".openEntry"), 500, {offset :-300});
            $('.openEntry').find('input[type=radio]').iCheck({'radioClass': 'styled-radio','checkedClass': 	'styled-radio-checked','insert': '<div class="check"></div>'}); 
			
        }
        else{}
    }
        else{
            $('.openEntry').slideUp(function(){$(this).remove()});
            $(this).removeClass('hovered active');
        }
    }
                  else{}
    });
});

/* #recommend-pkg-tabs */
$('#recommend-pkg-tabs > li').each(function(){
    //var $thisData = $(this).html();
    var $thisClass = $(this).attr('class');
    var $thisClick = $(this).not('a');
    
    if($thisClass.match(/[\w-]*group[\w-]*/g)){
        $("<li class='ajax-entry-rec list-view col-md-12 group'><div class='on-hover-details1'></div></li>").insertAfter(this);
    }
    else if($thisClass.match(/[\w-]*individual[\w-]*/g)){
    $("<li class='ajax-entry-rec list-view col-md-12 individual'><div class='on-hover-details1'></div></li>").insertAfter(this);
    }
    else{
    $("<li class='ajax-entry-rec list-view col-md-12'><div class='on-hover-details1'></div></li>").insertAfter(this);
    }
        
    $(this).on('click', function (event) {
         if(event.target.nodeName=='I' || event.target.nodeName=='A' || event.target.nodeName=='i' || event.target.nodeName=='a'){
            }
        else{
        if(!$(this).hasClass('active')){
            $(window).trigger('resize');
            $('.openEntryRec').slideUp();
            $('.hovered').removeClass('hovered active');
               $(this).addClass('hovered active');
            
        var previewRec = $(this);
        var previewDataRec = $(this).html();
        if(!$('.openEntryRec').length){
            previewRec.next('.ajax-entry-rec').clone().addClass('openEntryRec').html(previewDataRec).insertAfter(previewRec.nextAll('.last:first')).wrapInner( "<div class='on-hover-details1'></div>" ).height('auto').stop().css({'margin-top':-1}).slideDown();
            $('.on-hover-details1').find('.iCheck-helper, .check').remove();
            $('.openEntryRec').find('input[type=radio]').unwrap('.styled-radio')

            $.scrollTo($(".openEntryRec"), 500, {offset :-300});
            $('.openEntryRec').find('input[type=radio]').iCheck({'radioClass': 'styled-radio','checkedClass': 	'styled-radio-checked','insert': '<div class="check"></div>'}); 
        }
        else{}
    }
        else{
            $('.openEntryRec').slideUp(function(){$(this).remove()});
            $(this).removeClass('hovered active');
        }
        }
    });
});

    /*
$('#recommend-pkg-tabs').mouseleave(function() {
    $('.openEntryRec').stop().animate({'opacity':0},500,function(){$(this).fadeOut()}).remove();
    $(this).find('li').removeClass('hovered active');
})
*/
/*$(window).trigger('resize');
    $('body').on('click', '.close', function() {
        $('.openEntry').slideUp(800).remove();
    });
    */
$(window).trigger('resize');
});
/* 04/04/17 - Script for Listing Grid View Hover Effect */ 



/*Car Transfer : horizontal tabs*/
$('#Horizontal_Tab').easyResponsiveTabs({
	type:'default', //Types:default, vertical, accordion
	width:'auto', //auto or any width like 600px
	fit:true, // 100% fit in a container
	closed:'accordion', // Start closed if in accordion view
	tabidentify:'hor_2', // The tab groups identifier
	activate:function(event) {}
});
$('#Horizontal_Tab h2[aria-controls="hor_2_tab_item-0"]').click();


$(".customPlaceholder").click(function(){
    $(this).children(".customPlaceholder .styled-select").fadeIn();
    $(this).addClass("active");
});
/*Car Transfer : horizontal tabs*/
/*Car Transfer Within City : Drag Drop Section*/
$(".btn-closeSec").click(function(e){
        $(this).parents(".schedule-slot").remove();
        e.preventDefault();
    });
    $(document).delegate('.btn-closeSec','click',function(evt) {
       $(this).parents(".schedule-slot.selected").remove();
        $(".dragged:before, li.selected-box:before").css("display","none");
        $(".draggable-sect li").removeClass("dragged"); 
        evt.preventDefault();
    });
   
    $(".switchBtns a.btnMapView").click(function(e){
		e.preventDefault();
       $(".transfer-listing-content").hide();
       $(".cars.within-city .mapView").show(); 
       $(".switchBtns a.btnMapView").css("display","none");
       $(".switchBtns a.btnListView").css("display","block");
    });
    $(".switchBtns > a.btnListView").click(function(e){
		e.preventDefault();
       $(".transfer-listing-content").show();
       $(".cars.within-city .mapView").hide(); 
        $(".switchBtns a.btnListView").css("display","none");
        $(".switchBtns a.btnMapView").css("display","block");
    });
/*Car Transfer Within City : Drag Drop Section*/
/*Car Transfer Within City : Car Result Sec*/
$(document).on("click",".cars .list-view .place-attractions-right a.upgrade-car", function(e) {
        e.preventDefault();
        $(this).addClass('active');
        $(".more-details").removeClass("active");   
        $(this).parents('li').find('.pkg-similar-list').slideDown();
        $(this).parents('li').find('.prod-more-info').slideUp(); 
    return false;
});

$(document).on("click",".pkg-similar-list .close-sec", function(e) {
    $(this).parents('li').find('.pkg-similar-list').slideUp();
    $(".upgrade-car").removeClass("active");    
    e.preventDefault();
});

/*Car Transfer Within City : Car Result Sec*/
/*Car Transfer Within City : Car Details Sec*/



DragDropActivity();

/*Cars  - More details starts*/   
    $(document).on("click",".cars .list-view .place-attractions-right a.more-details", function(e) {
   $(this).parents('li').find('.prod-more-info').slideDown(); 
   $(this).parents('li').find('.pkg-similar-list').slideUp();
   $(this).addClass("active");  
   $(".upgrade-car").removeClass("active");
    e.preventDefault();
});

$(document).on("click",".prod-more-info .close-sec", function(e) {
   $(this).parent().slideUp();
    /*$(this).parent().parent(".more-details").removeClass("active"); */
    $(".more-details").removeClass("active");    
    e.preventDefault();
});

$(document).on("click",".rcmd-extrs .close-sectn", function(e) {
$(this).parent().slideUp();
$(".recomended-btn").removeClass("active");     
});

$(document).on("click",".view-map a.btn-map", function(e) {
    e.preventDefault();
    $(this).toggleClass("active");
    $(this).parent().find(".view-map-larger").toggle();
    $(this).parents('.pkg-listing').find('li').css({'z-index':'1'})
    $(this).parents('li').css({'z-index':'2222'})
});

$(document).on("click",".button-close", function(e) {
    //$(this).toggleClass('active');
	e.preventDefault();
    $(this).parents('.view-map-larger').hide();
	$('.view-map > .btn-map').removeClass('active');
});
/*Cars - Within City - More details Ends*/

$(".custom-close").click(function(e){
    //$(this).toggleClass('active');
	e.preventDefault();
    $(this).parents('.view-map-larger').hide();
	$('.view-map > .btn-map').removeClass('active');
});

$('.form-des-element').click(function(e){
    e.preventDefault();
    $('.form-des-details').hide();
    $(this).parent().find('.form-des-details').show();
});
    
$('#CarTransfer input').on('ifChecked', function(){
    if($('input[name="car-way"]:Checked').val() == "Oneway"){
        $('input[name="car-share"]').iCheck('enable');
    if(($('input[name="car-share"]:Checked').val() == "Share")&&($('input[name="car-way"]:Checked').val() == "Oneway")) {
       $('#CarTransfer .car-form-element').hide();
       $('#oneway-share').show();
    }
    else if(($('input[name="car-share"]:Checked').val() == "Non Share")&&($('input[name="car-way"]:Checked').val() == "Oneway")) {
        $('#CarTransfer .car-form-element').hide();
       $('#oneway-nonshare').show();
    }
    }
    else if($('input[name="car-way"]:Checked').val() == "Return") {
       $('#CarTransfer .car-form-element').hide();
       $('#return').show();
       $('input[name="car-share"]').iCheck('uncheck').iCheck('disable');
    }
});

$(".sel-seats-wrap").hide(); 
$(document).on("click",".btn-seats-sel", function(e) {
    var detaiButtnPos= $(this).offset().top;   
    e.preventDefault();
    $(this).toggleClass("active");
    $(this).parents('.list-view.pkg-listing').find('li').css({ zIndex: 2});
    $(this).parents('li').find(".sel-seats-wrap").toggle();    
    $(this).parents('li').find(".sel-seats-wrap").offset({ top: detaiButtnPos+32});
    $(this).parents('li').css({ zIndex: 3});
}); 

$(document).on("click",".car-select-options + .car-select-options .option-list > li > a", function(e) {
    e.preventDefault();
    $(this).toggleClass("selected");
    $(this).parent().toggleClass("active");
    //$(this).parents('li').addClass("active");
});

$(document).on("click",".sel-seats-wrap a.button-close", function(e) {
    e.preventDefault();
    $(".sel-seats-wrap").hide(); 
    $(".btn-seats-sel").removeClass("active");
});

$(document).on("click",".seat-one", function(e) {
    e.preventDefault();
    $('.seat-one').toggleClass("selected");
    $('.seat-one').parent().toggleClass("active");
    $('.car-seats-legends > li + li + li').show();
    $('.sel-seats-wrap .rht-sec').show();
});

$(document).on("click",".seat-two", function(e) {
    e.preventDefault();
    $('.seat-two').toggleClass("selected");
    $('.seat-two').parent().toggleClass("active");
    $('.car-seats-legends > li + li + li').show();
    $('.sel-seats-wrap .rht-sec').show();
});

$(".car-select-options + .rht-sec.center").hide();
$(document).on("click",".seat-three", function(e) {
    e.preventDefault();
    $('.seat-three').toggleClass("selected");
    $('.seat-three').parent().toggleClass("active");
    $('.car-select-options + .rht-sec.center').toggle();
    $('.car-seats-legends > li + li + li').show();
    $('.sel-seats-wrap .rht-sec').show();
});
/* Cars 27-01-17 */

$('.horizontaltab-reserv').responsiveTabs({
	startCollapsed: 'accordion',
	collapsible: 'accordion'
});

/*Car Transfer Within City : Car Details Sec*/

/*Car index : Start  ON 12-4-17*/

$('.form-des-element').click(function(e){
    e.preventDefault();
    $(this).closest('ul').find('.form-des-details').hide();
    $(this).closest('li').find('.form-des-details').show();
}); 
$(".customPlaceholder").click(function(){
    $(this).children(".customPlaceholder .styled-select").fadeIn();
    $(this).children(".customPlaceholder > span").hide();
    $(this).addClass("active");
    
});
/*$('#Horizontal_Tab h2[aria-controls="hor_2_tab_item-0"]').click();*/

/*Car transfer start*/
$('#CarTransfer input').on('ifChecked', function(){
    if($('input[name="car-way"]:Checked').val() == "Oneway"){
        $('input[name="car-share"]').iCheck('enable');
    if(($('input[name="car-share"]:Checked').val() == "Share")&&($('input[name="car-way"]:Checked').val() == "Oneway")) {
       $('#CarTransfer .car-form-element').hide();
       $('#oneway-share').show();
    }
    else if(($('input[name="car-share"]:Checked').val() == "Non Share")&&($('input[name="car-way"]:Checked').val() == "Oneway")) {
        $('#CarTransfer .car-form-element').hide();
       $('#oneway-nonshare').show();
    }
    }
    else if($('input[name="car-way"]:Checked').val() == "Return") {
       $('#CarTransfer .car-form-element').hide();
       $('#return').show();
       $('input[name="car-share"]').iCheck('uncheck').iCheck('disable');
    }
});
/*Car transfer end*/

/*Car with/multy-city start*/
$('.chekbox-radio-group input').on('ifChecked', function(){
    if($('input[name="car-pkg"]:Checked').val() == "WithinCity"){
		$('#WithinCity').show();
        $('#Multicity').hide();
     }	
	if($('input[name="car-pkg"]:Checked').val() == "Multicity"){
		$('#WithinCity').hide();
		$('#Multicity').show();
    }	
	
});
/*Car with/multy-city end*/

/*Car Add/remove city start*/

$(".multi-rw-cont").each(function(){
    var personclone = $(this).find(".cust-row-repeat"), TargetDiv = $(this);
    
    //** Add Button **/
    $(this).find(".add-row-room").click(function(e){
                e.preventDefault();
        var TargetElem = TargetDiv.find('.cust-row-repeat'),
            num = TargetElem.length,
            CloneElem = TargetElem.not('.clone-sec').clone(),
            Elemlabel = TargetDiv.find('.lft-lbl-room');
        
        if(num<5){
            TargetElem.removeClass('ajaxform');
            CloneElem.appendTo(TargetDiv.find('.add-multicity')).addClass("clone-sec ajaxform").find('input').not('.spinner input').attr({'id':''}).attr('placeholder');
            $(".clone-sec.ajaxform").find('.spinner input').val('1');
            if(Elemlabel.length){
                TargetDiv.find('.ajaxform').find('.lft-lbl-room').children('span').text((ElemLabelCount+1));
            }
          
            initAjaxForms();
            
            if(num==4){
                $(this).parent().hide();
                }
            else{
                $(this).parent().show();
              }
            }
        else{}
    });
    //** Remove Button **/
       $(this).delegate(".del-row-room", 'click', function (e) {
       e.preventDefault();
        
        var TargetElem = TargetDiv.find('.cust-row-repeat'),
             TargetElemRemove = $(this).closest('.clone-sec'),
              numRemove = TargetElem.length;
        
        TargetElemRemove.remove();
        
        console.log(numRemove);
        
         if(numRemove<6){
                TargetDiv.find('.add-row-room').parent().show();
         }
        else{
            //alert('bye');
        }
    });
});
/*Car Add/remove city end*/

/*Add activity - Itinerary hide-show start - Vishal*/
$('.btn-itinerary').click(function(e){
	e.preventDefault();
	$('.activity-list,.brief-itinerary').toggle();
})
/*Add activity - Itinerary hide-show end*/


/*Car index : End*/

/*Plugin-UI date picker*/
 $( function() {
    $( ".datepicker" ).datepicker();
  } );
/*Plugin-UI date picker Ends*/
  

/**** My Account Module Starts ******/
/*Plugin-secondary navigations*/
/***** active link***/
$(".aside-widget a").each(function () {
//$(this).addClass('activemenu');
   if (this.href == document.URL) {
        $(this).addClass('active');
        $(this).attr('href', 'javascript:void();');
    }
});

/** Aside Toggle **/
if($('.aside-widget').length){
    $('.aside-widget').each(function(){
    var $TargetAside = $(this), 
        $BtnAside = $TargetAside.find('.aside-btn'),
        $ContAside = $TargetAside.find('.aside-container'),
        $BtnActiveAside = $ContAside.find('.active').text();
        $BtnAside.find('a').text($BtnActiveAside);
    })
}
else{
}
$('.aside-btn a').click(function( event ) {
	event.preventDefault();
	$('.left-panel .aside-container').slideToggle();
	$( this ).toggleClass( "active" );
});
/*Plugin-secondary navigations Ends*/



/*plugin-Easy Responsive Tabs*/
$(document).ready(function () {
    
    $('.parentHorizontalTab').easyResponsiveTabs({
        type: 'default', //Types: default, vertical, accordion
        width: 'auto', //auto or any width like 600px
        fit: true, // 100% fit in a container
        closed: 'accordion', // Start closed if in accordion view
        tabidentify: 'hor_1', // The tab groups identifier
        activate: function (event) { // Callback function if tab is switched
            var $tab = $(this);
            var $info = $('#nested-tabInfo');
            var $name = $('span', $info);
            equalheight('.equal-heights > div,.equal-heights > li');
           //$name.text($tab.text());

            //$info.show();
        }
    });
    
    $('.ChildVerticalTab_1').easyResponsiveTabs({
        type: 'default',
        width: 'auto',
        fit: true,
        tabidentify: 'ver_1', // The tab groups identifier
        activetab_bg: '#fff', // background color for active tabs in this group
        inactive_bg: '#F5F5F5', // background color for inactive tabs in this group
        active_border_color: '#c1c1c1', // border color for active tabs heads in this group
        active_content_border_color: '#5AB1D0' // border color for active tabs contect in this group so that it matches the tab head border
    });
    
});
/*plugin-Easy Responsive Tabs Ends*/
/*plugin-accordin list starts*/
var initAccordinCont = function() {
$('.accordion-style dt').click(function(){ 
	$(this).next("dd").slideToggle();
	$(this).toggleClass('active');
});
}
/*plugin-accordin list ends*/

$('.login-nav .colapsed').click(function(e){e.preventDefault();$('.login-list').slideToggle();$(this).toggleClass('expanded');$(this).toggleClass('colapsed');});



/** required Fields **/
$('.req-field').each(function(){
var $this = $(this), ReqHolder = $this.find('.holder'), thisText = $this.find('input').val(), thisfield = $this.find("input");
// initialisation
if(thisText.length){ReqHolder.fadeOut();}
else{ReqHolder.fadeIn();}
//focus
thisfield.focus(function(){ReqHolder.fadeOut();	}).focusout(function(){
    if($this.find('input').val().length){ReqHolder.fadeOut();}
    else{ReqHolder.fadeIn();}
});
});	

/*My Profile*/
$(".edit-profile-link").click(function(e){
    $(".edit-my-profile").show();
    $(".my-profile-tab").hide();
    e.preventDefault();
});
$(".save-my-profile").click(function(e){
    $(".edit-my-profile").hide();
    $(".my-profile-tab").show();
    e.preventDefault();
});
$('.my-profile .parentHorizontalTab .resp-tab-item.resp-tab-active').removeClass('resp-tab-active');
/*My Profile*/

/*My-Bookings-Upcoming*/
$(".paymnet-details").click(function(e){
    e.preventDefault();
    $(this).closest('.content').find('.payment-details-block').slideToggle("slow");
    $(this).toggleClass("active");
})
//$(".btn-view-details").click(function(e){
//    $(".view-details-block").show();
//    $(this).parent().parent().addClass('removebtmbdr');
//    e.preventDefault();
//});

$(".close-container > a").click(function(e){
    e.preventDefault();  
    $(".view-details-block").hide();
})
$(".top-link .btn-close").click(function(e){
    e.preventDefault();  
    $(".view-details-block").hide();
})
/*My-Bookings-Upcoming*/

$("#otp-auth .btn-contr > .btn").click(function(e) {
    $("#otp-auth").hide();
    $('.parentHorizontalTab_1 > .resp-tabs-list > .resp-tab-item.resp-tab-active').removeClass('resp-tab-active');
});
$('.parentHorizontalTab_1 > .resp-tabs-list > .resp-tab-item.resp-tab-active').removeClass('resp-tab-active');
                             
/*$(".btn-view-details").bind('touchend , click', function(e) {
	e.preventDefault();
	if($(this).hasClass('active')){
		$(".avail-details").stop().slideUp();
		$(this).removeClass("active");
	}
	else{
		$(this).addClass("active");
		$(".avail-details").stop().slideDown();
	}
});*/
    
$('.close-view-details').click(function(){
    $(this).parent('.avail-details').slideUp();
    $('.aval-dtl-sec .btn-normal a.btn-view-details').removeClass("active");
    return false;
});

/* My Gift Cards : Starts */
$(".toggle-button").click(function(e) {
    var ToggleDataHeight = $(this).parent('.toggle-data-wrap').find('.toggle-data').outerHeight();
    e.preventDefault();
    
    $(this).closest('td').addClass('popup-opned');
    
    if(!$(this).parents().hasClass('my-gifts-cards')){
        $('.toggle-data').hide();
        if(!$(this).hasClass('active')){        
            $(this).addClass('active');
            $(this).parent('.toggle-data-wrap').find('.toggle-data').slideDown();
            $(this).parent('.toggle-data-wrap').parent('div').animate({'padding-bottom': ToggleDataHeight + 10},400).toggleClass('collapsed');
        }
        else{
            $(this).removeClass("active");        
            $(this).parent('.toggle-data-wrap').find('.toggle-data').slideUp();
            $(this).parent('.toggle-data-wrap').parent('div').animate({'padding-bottom': 0},400);
        }       
    }else{
        //alert('my-gifts-cards');
        var ExtendValidDataHeight = $('.popup-opned').find('.extend-valid-data').outerHeight()+30;
        
        $(this).parents('.popup-opned').find('.extend-valid-data').slideDown();	
        $(this).parents('.popup-opned').css("padding-bottom",ExtendValidDataHeight);
        $(this).parents('.popup-opned').siblings().css("padding-bottom",ExtendValidDataHeight);
    }
});
    
$('.toggle-data .btn-close').click(function(ev) {
    ev.preventDefault();
	if(!$(this).parents('td').hasClass('popup-opned')){
        $('.toggle-button').removeClass('active');
        $(this).parent('.toggle-data').slideUp();
        $(this).parents('.toggle-data-wrap').parent('div').animate({'padding-bottom': 0},400).removeClass('collapsed');
    }else{
        $('.toggle-button').removeClass('active');
        
        $(this).parents('.popup-opned').find('.txt-link').addClass('.popup-inline').removeClass('.toggle-button');	
        $(this).parents('.popup-opned').find('.extend-valid-data').slideUp();	
        $(this).parents('.popup-opned').css("padding-bottom",'0').siblings().css("padding-bottom",'0');
        $(this).parents('.popup-opned').removeClass('popup-opned');
    }
});
/* My Gift Cards : Starts */

/* My Wallet Gift Cards : Starts */
$(".btn-buy-card").click(function(e){	
    $(".gift-cards-list, .card-mgmt-banner, .gift-card-wrap, .gift-a-card-sec").hide();
	$(".buy-a-card-sec,.buy-a-card").show(); 
    e.preventDefault();
    //equalheight('.equal-heights > div, .equal-heights > li');
});
    
//$(".btn-buy-card-back").click(function(e){
//    $(".buy-a-card").hide(); 
//    $(".gift-cards-list").show();
//    e.preventDefault();
//    equalheight('.equal-heights > div, .equal-heights > li'); 
//});

$(".btn-gift-card").click(function(e){
    $(".gift-a-card-sec").show(); 
    $(".buy-a-card, .card-mgmt-banner, .gift-card-wrap, .gift-cards-list").hide();
    e.preventDefault();
    //equalheight('.equal-heights > div, .equal-heights > li');
});
//above breadcum Package bar Hide Show
$("#addvalue").click(function(){
   $(".gift-cards .pkg-bar").css("display","block");
   $(".added-amt").show();
   $(".blnk-amt").hide();
});
    
$(".btn-gift-card-back").click(function(e){
    $(".gift-a-card").hide(); 
    $(".gift-cards-list").show();
    e.preventDefault();
    equalheight('.equal-heights > div, .equal-heights > li');
});
/* My Wallet - Gift Cards : Ends */

/*Report loss Damage : Start */
$(".report-link > a").click(function(e){
	e.preventDefault();
    $(".report-loss-damage").show(); 
    $(".my-gifts-cards, .faclt-tab").hide();
	$('.resp-tab-item.hor_1.resp-tab-active').addClass('removebtmbdr');
});
  $(".btn-loss-damage-back").click(function(e){
    e.preventDefault(); 
    $(".report-loss-damage").hide(); 
    $(".my-gifts-cards, .faclt-tab").show();  
  });  
    
/*Report loss Damage : Ends */

/* Priority : Starts */
$(".popup-container .popup-box ul.priority-list li").click(function(){
    $(".popup-container .popup-box ul.priority-list li").removeClass("selected");
    $(this).addClass("selected")
});

$('.popup-container .popup-box .action').click(function(){
   $(this).parents().find('.popup-container').hide();
});

$('#priority .btn-contr > .btn').click(function(evv){
    evv.preventDefault();
    $('.has-after-pri-set').hide();
    $('.after-pri-set').show();
});

$('.col-set-priority > div > .btn-white,  .after-pri-set > .btn-edit').click(function(e){
    e.preventDefault();
    $('#priority').show();
});
/* Priority : Starts */
/* Newly added */
$('.wallet-details .add-money').click(function(){
    $('.wallet-sect .wallet-box').hide();
    $('.wallet-sect .ver-tab-container').show();
});

$('.wallet-sect  .ver-tab-container .pay-now-btn').click(function(){
    $('.wallet-sect .ver-tab-container, .btn-blue, .aval-dtl-txt').hide();
    $('.wallet-sect .successfull-payment, .aval-dtl-txt.added').show();
    $('.resp-tab-item.hor_1.resp-tab-active').addClass('removebtmbdr');
	$('.my-wallet .simple-tab-list li a').removeClass('active');
});

$('.pay-now-btn').click(function(){
    $('.aval-dtl-txt, .actual-value').hide();
    $('.aval-dtl-txt.added, .value-added').show();
});

$('.gift-card').on('ifChecked', function(){
	$('#gift-card-sec').show();
	$('#gv-card-sec').hide();
});

$('.e-gvcard').on('ifChecked', function(){
	$('#gift-card-sec').hide();
	$('#gv-card-sec').show();
});
$('.same-add-no').on('ifChecked', function(){
	$('.shipping-dtls').show();
});
$('.same-add-yes').on('ifChecked', function(){
	$('.shipping-dtls').hide();
});

/**** My Account Module Ends ******/
$("input[placeholder]").each(function () {
    $(this).attr("data-placeholder", this.placeholder);

    $(this).bind("focus", function () {
        this.placeholder = '';
    });
    $(this).bind("blur", function () {
        this.placeholder = $(this).attr("data-placeholder");
    });
});


$('.flexible-dates .owl-carousel').owlCarousel({
    loop:false,
    center:true,
    navigation:true,
    pagination :false,
	afterAction: function(el){
   //remove class active
   this
   .$owlItems
   .removeClass('selected')

   //add class active
   this
   .$owlItems //owl internal $ object containing items
   .eq(this.currentItem + 3)
   .addClass('selected')    
    }, 
    itemsDesktop : [2000,7],
    itemsDesktopSmall : [1199,5],
    itemsTablet : [991,3],
    itemsMobile : [599,1]
});

/* 09-05-17 */
var initFloatingHeights = function() {
    $('.floating-btns > ul > li').each(function(){
        var FloatingBtnsHeights = $(this).outerHeight();
        
        $('.floating-btns > ul > li > a').click(function(e){
            e.preventDefault();
            if(!$(this).hasClass('active')){
                $('.floating-btns > ul > li > a').removeClass('active');
                $(this).addClass('active');
                $(this).parents('ul').find('.Float-box').hide();
                $(this).next('.Float-box').show();
            }else{
                $(this).removeClass('active');
                $(this).parents('ul').find('.Float-box').hide();
            }
        });
        
        $('.close-btn').click(function(e){
            e.preventDefault();
            $(this).parents('.Float-box').toggle();
            $(this).parents('li').find('.active').toggleClass('active');
        });

        $('#msgSec').click(function(ev){
            ev.preventDefault();
            $(this).parents('ul').find('.Float-box').hide();
            $('.enquiry-form-sucess').toggle();
        });
        
        $('#chatSec').click(function(e){
            e.preventDefault();
            $(this).parents('ul').find('.Float-box').hide();
            $('.chat-sec').toggle();
        });
    });
}
 
    var ww = document.body.clientWidth;
        if (ww <= 767){
            /*alert('Less than 960');*/
            $(document).on('click','.floating-btns > ul > li > a, #chatSec, #msgSec', function(event) {
    var targetDiv = $(this).attr("href");
    event.preventDefault();
    $.magnificPopup.open({
	type:'inline',
   items:{ src : targetDiv},
	fixedContentPos:true,
	fixedBgPos:true,
	overflowY:'auto',
	closeBtnInside:true,
	preloader:false,
	midClick:true,
	removalDelay:300,
	mainClass:'my-mfp-slide-bottom'
});
        });
        }
    else {
        $('.Float-box').hide();
        initFloatingHeights();
    }


/* 09-05-17 */

/*10-05-2017 : Multi-Product-Checkout*/
$('.check_box').on('ifChecked', function(event){
    $(this).closest(".ancillary-srvcs > ul > li").find(".form-group-sec").toggleClass("active");
    $(this).closest(".ancillary-srvcs.type-02 > ul > li").find(".room-price").toggleClass("active");
});     
$('.check_box').on('ifUnchecked', function(event){
    $(this).closest(".ancillary-srvcs > ul > li").find(".form-group-sec").removeClass("active");
	$(this).closest(".ancillary-srvcs.type-02 > ul > li").find(".room-price").removeClass("active");
});     
/*10-05-2017 : Multi-Product-Checkout*/

/*10-05-2017 : General - Login  */
$('.btn-login').click(function(e){
    e.preventDefault();
    $(".after-lgn").hide();
    $(".before-lgn").show();
    $(".before-lgn").css("display","inline-block");
});

$('.btn-logout').click(function(e){
    e.preventDefault();
    $(".after-lgn").show();
    $(".before-lgn").hide();
});
/*10-05-2017 : General - Login  */

/*11-05-2017 : All-Product-Checkout Travellers*/
$("#roomFilledMsg").on('ifChecked', function(event){
    //alert("hi");
    $("input.disabled-checkbox").prop("disabled", true);
    $("input.disabled-checkbox").parent().parent().addClass("disabled");
    $(".popup-box.saved-traveler-popup .popup-in .btn.btn-blue").removeClass("disabled");
    $(".popup-box.saved-traveler-popup .popup-in h5.headTl").hide();
    $(".popup-box.saved-traveler-popup .popup-in h5.filledMsg").show();
    
});

$(".close-popup").click(function(){
    $('.mfp-close').trigger('click');
});
/*11-05-2017 : All-Product-Checkout Travellers*/

/* 11-05-2017 : Sidebar navigation to select dropdown */
/** Aside Toggle **/
$(".hrtabs-panel .resp-tabs-list a, .tab2-sec .resp-tabs-list a").each(function () {
   if (this.href == document.URL) {
       /*   if (this.href.) {
		$(this).attr('href', 'javascript:void();');
    }
        else{*/
         $(this).addClass('active').parent().addClass('resp-tab-active');
           // alert('hi');
		$(this).attr('href', 'javascript:void();');
        //}
    }
});

if($('.hrtabs-panel, .tab2-sec').length){
    $('.hrtabs-panel, .tab2-sec').each(function(){
    var $TargetAside = $(this), 
        $BtnAside = $TargetAside.find('.aside-btn'),
        $ContAside = $TargetAside.find('.resp-tabs-list'),
        $BtnActiveAside = $ContAside.find('.active').text();
    
    $BtnAside.find('a').text($BtnActiveAside);
    })
}
else{
}

$('.aside-btn a').click(function( event ) {
	event.preventDefault();
	$('.hrtabs-panel .resp-tabs-list, .tab2-sec .resp-tabs-list').slideToggle();
});
/* 11-05-2017 : Sidebar navigation to select dropdown */

/*Group Flow - Holiday Customise Pre Tour - Left panel 12-05-2017*/
$(".select-room-price").click(function(){
    var OldPrcFigure = $('.pkg-bar-list .price .total-amt').text().replace(/,/g, "").replace(",", ""),
    AddPrcFigure = $(this).closest('.room-details-2 > li, .room-details > li').find(".btn-upgrade > .btns > span.price-amt > .room-price").text().replace(",", "").replace("*", "");
    var NewPrcFigure=addnumbers(OldPrcFigure,AddPrcFigure);
     //alert(NewPrcFigure); 
    $('.total-amt').text(NewPrcFigure).digits().parent().addClass('pr-updated'); 
});
/**/
/*Group Flow - Holiday Customise Pre Tour - Left panel 12-05-2017*/
/*Group Flow - Holiday Customise Pre Tour - Right panel 12-05-2017*/
$(".select-room-prices").click(function(){
    var OldPrcFigure = $('.pkg-bar-list .price .total-amt').text().replace(/,/g, "").replace(",", ""),
    AddPrcFigure = $(this).closest('.room-details > li').find(".btn-upgrade > .btns > span.price-amt > .room-price").text().replace(",", "").replace("*", "");
         //alert(OldPrcFigure); 
    var NewPrcFigure=addnumbers(OldPrcFigure,AddPrcFigure);

    $('.total-amt').text(NewPrcFigure).digits().parent().addClass('pr-updated'); 
});
/**/
/*Group Flow - Holiday Customise Pre Tour - Right panel 12-05-2017*/

/*Group Flow - Booking Summary*/
$(".filter-search-panel-toggle").click(function(){
    $(this).toggleClass('active');
    $('#ul-links-mobile').slideToggle();
    $(".filter-search-panel-toggle i").toggleClass("fa-angle-up fa-angle-down");
});
/*Group Flow - Booking Summary*/
/*Group Flow - Holiday Customise - Left panel*/
//Room Details > btn-select

$(document).on('click','.room-details-2 > li .btn-select',function(){              
var el = $(this);
var el_Li = el.parents('li');
el_Li.addClass('selected');                
el.parent().hide();
//debugger;
$('.r-tabs-state-active .pre-post-cont').show();

});


/*Group Flow - Holiday Customise - Left panel*/
/*Group Flow - Holiday Customise - Left panel - Checkbox*/
$(".chkRooming").on('ifUnchecked', function(event){
    $(this).closest(".room-details-2 > li").find(".pre-tour-passengers > .select-no-of-pax").slideDown();
});
$(".chkRooming").on('ifChecked', function(event){
    $(this).closest(".room-details-2 > li").find(".pre-tour-passengers > .select-no-of-pax").slideUp(); 
});
/*Group Flow - Holiday Customise - Left panel - Checkbox*/
/*Group Flow - Holiday Customise - Right panel*/

$(".priority-list > li").hover(function(){
    $(".priority-list > li:first-child").removeClass("active");
    $(this).toggleClass("active");
});  
$(".group .room-details .btn-upgrade + span a.select-room").click(function(){
    $(this).parent().hide();
    $(".group .room-details .btn-upgrade + span + .btn-check").show();
    $(".already-incl").hide();
    $(".holidays.customise-trip.group .details-sec .hotel-dsc").hide();
});
$(".holidays.group ul.priority-list li .btn.btn-blue").click(function(){
   $('.mfp-close').trigger('click');
    $(".holidays.customise-trip.group .details-sec .hotel-dsc").hide();
    $(".hotel-upgraded").show();
    $(".upgrad-action").hide();
    $(".customize-trip-list > li:first-child .schedule-details > ul > li:first-child .iti-details").removeClass("selected");
});
/*Group Flow - Holiday Customise - Right panel*/


/* 15/05/17 - Script for Listing Grid View Click Effect */ 
$('#pkg-listings-b2b > li figure .btn-download-pdf').click(function(evt){evt.preventDefault()});

$(function() {

$(window).on('resize', function() {
    if($('#pkg-listings-b2b').length){
    $('.openEntry').remove();
    $('.ajax-entry').hide();

    var startPosX = $('#pkg-listings-b2b > li:first').position().left;
    //console.log(startPosX);
    $('.ajax-mast, #pkg-listings-b2b > li').removeClass("first last");
    $('.ajax-entry').each(function() {
        if ($(this).prev('li').position().left == startPosX) {
            $(this).prev('li').addClass("first");
            $(this).prevAll('.ajax-entry:first').addClass("last");
        }
    });
    $('.ajax-entry:last').addClass("last");
    }
});
    
$('#pkg-listings-b2b > li').each(function(){
    var $thisData = $(this).html();
    $("<li class='ajax-entry list-view col-md-12'><div class='on-hover-details1'>"+$thisData+"</div></li>").insertAfter(this);

    $(this).find('a.btn-download-pdf').click(function(evtt) {
        evtt.preventDefault();
        //$('.hovered').removeClass('hovered');
        $(this).closest('li').addClass('hovered clicked');
        $(window).trigger('resize');
        
        $('.openEntry').slideUp(800);
        var preview = $(this).closest('li');
        if(!$('.openEntry').length){
            preview.next('.ajax-entry').clone().addClass('openEntry').insertAfter(preview.nextAll('.last:first')).stop().slideDown();
            //console.log('0000');
			$('.submitRequest').click(function(evv){
				evv.preventDefault();
				$(this).parents(".full-width").find('.downloadPdfForm').hide();
				$(this).parents(".full-width").find('.request-thank').show();
			});
        }
        else{
            $('.openEntry').remove();
            preview.next('.ajax-entry').clone().addClass('openEntry').insertAfter(preview.nextAll('.last:first')).stop().slideDown();
            //console.log('1111');
			$('.submitRequest').click(function(evv){
				evv.preventDefault();
				$(this).parents(".full-width").find('.downloadPdfForm').hide();
				$(this).parents(".full-width").find('.request-thank').show();
			});
        }
        // alert('hi');
    });
    
});
    
$(document).on('click','#pkg-listings-b2b .btn-close', function(ev){
	ev.preventDefault();
    $('#pkg-listings-b2b .openEntry').stop().slideUp(function(){$(this).remove();})
    $(this).parents("#pkg-listings-b2b").find('li').removeClass('hovered');
});

/*$('#pkg-listings-b2b ').mouseleave(function() {
    $('.openEntry').stop().slideUp().remove();
        $(this).find('li').removeClass('hovered');
});*/

$(window).trigger('resize');
});

/*$('.downloadPdfForm .btn-blue.submit').click(function(evv){
    evv.preventDefault();
    alert('HIHIH');
    $(this).parents('.downloadPdfForm').hide();
    $(this).parents('.downloadPdfForm').next('.thankSec').show();
});*/
/* 15/05/17 - Script for Listing Grid View Click Effect */ 



/* 17-may-2017 Feedback start */ 

$('.parentHorizontalTab_1').easyResponsiveTabs({
    type: 'default', //Types: default, vertical, accordion
    width: 'auto', //auto or any width like 600px
    fit: true, // 100% fit in a container
    closed: true, // Start closed if in accordion view
    tabidentify: 'hor_1', // The tab groups identifier
    activate: function (event) { // Callback function if tab is switched
        var $tab = $(this);
        var $info = $('#nested-tabInfo');
        var $name = $('span', $info);
        equalheight('.equal-heights > div,.equal-heights > li');
        //$name.text($tab.text());
        //$info.show();
    }
});
    
//$('#parentHorizontalTab_1 .resp-tab-item.resp-tab-active').removeClass('resp-tab-active');
	
$('#ChildHorizontalTab').easyResponsiveTabs({
        type: 'default',
        width: 'auto',
        fit: true,
        tabidentify: 'child_hor', // The tab groups identifier
        activetab_bg: '#fff', // background color for active tabs in this group
        inactive_bg: '#F5F5F5', // background color for inactive tabs in this group
        active_border_color: '#c1c1c1', // border color for active tabs heads in this group
        active_content_border_color: '#5AB1D0' // border color for active tabs contect in this group so that it matches the tab head border
     });


	
    $("#submit").click(function(){
    $("#complaint-submit").show();
    $("#complaint").hide();
    });
    
    $("#submit2").click(function(){
    $("#before-submit").show();
    $("#before-trip").hide();
    });
	
	$("#submit3").click(function(){
    $("#after-submit").show();
    $("#during-trip").hide();
    });
    
    
    $('.recommend-yes').on('ifChecked', function(){
	$('.share-details').show();
});
    $('.recommend-no').on('ifChecked', function(){
	$('.share-details').hide();
    
});

$(".remarks-btn").click(function(){
	$(this).toggleClass("active");
    $(this).parents('ul.genrl-feedback li').find('.remarks-cnt').slideToggle();
    $(this).parents('ul.collapse-container li').find('.remarks-cnt').slideToggle();
    
    
});
    

/* 17-may-2017 Feedback end */ 

/*19-05-2017 : Cars Ammedment : 02-Self-Drive-Change-Date-and-Timing*/


/* $('#Horizontal_Tab h2[aria-controls="hor_2_tab_item-1"]').click();*/
/*19-05-2017 : Cars Ammedment : 02-Self-Drive-Change-Date-and-Timing*/
/*19-05-2017 : Activity Ammedment : 01b-Activity-Add-Traveler*/
$('.add-sec .count-up').click(function(){
		$('.btn-sec').show();
	});
	$('.btn-add').click(function(){
		
		if($('.txt-adult').val() < 2)
		{			
			window.location.href = 'activity-add-traveler-checkout-travelers.shtml'; 			
		}
		else
		{			
			window.location.href = 'activity-add-traveler-error.shtml'; 
		}		
	});
/*19-05-2017 : Activity Ammedment : 01b-Activity-Add-Traveler*/
/*31-05-2017 : Destination : Destination Search*/
$(".destinations-list .dest-tl").click(function(){
    $(this).toggleClass("active");
    $(".destinations-dropdown").slideToggle();
});
/*31-05-2017 : Destination : Destination Search*/

/*1-06-2017 : Destination : Destination Search*/
$('.dest-map.map-panel > figure').click(function(){
	$(this).parent().find('.map-hotel-popup').toggle();
});
/*1-06-2017 : Destination : Destination Search*/

/*07-06-2017 Holiday Common*/
$(".btn-upgrade-sec").click(function(e){
    $(this).addClass("disabled");
    $(".upgrade-sec").slideToggle(); 
    $(this).parents().addClass("selected");
    e.preventDefault();
}); 
   $(".chk-extend .checkbox-inline").click(function() {	
    $(".roming-config").slideToggle()(300);
});
    
    $(".upgrade-flight-btn").click(function(e){
        $(".cust-ext-nights-added .pkg-bar .extranights-added").hide();
        $(".holidays .pkg-bar .flight-upgraded").show();
        $(".view-switch .price .old-price").hide();
        $(".view-switch .price .upgraded-price-flight").show();
        $(this).hide();
        $(this).parents(".details-sec .hotel-dsc .room-details li").addClass("selected");
        $(".room-details .upgrade-btn  span.tick-selected.upgraded-txt").show();
        $(".schedule-details .iti-details .iti-info .iti-tl .upgraded-flght-type").show();
        $(".details-sec .hotel-dsc .room-details li > span.close-sec").show();
        $(".schedule-details .iti-details .iti-info .iti-tl .old-flght-type").hide();
        e.preventDefault();
    });
    $(".close-rt").click(function(){
        $(".upgrade-sec").slideUp();
        $(".btn-upgrade-sec").removeClass("disabled");
        
    })
/*07-06-2017 Holiday Common*/

/////// 07-06-2017 Flights-hotels Start : Homepage////////
	$('.fl-one-way').on('ifChecked', function (event){		
    	$('.srch-oneway-return').show();
		$('.fh-multicity').hide();   
		$('.cf-return').addClass("disabled");
		$(".cf-return input").attr("disabled","disabled");
		$('.btn-oneway').show(); 
		$('.btn-return').hide();
        $('#flight-hotels').hide();
	});
	$('.fl-return').on('ifChecked', function (event){			
    	$('.srch-oneway-return').show();
		$('.fh-multicity').hide();   
		$('.cf-return').removeClass("disabled");
        $(".cf-return input").removeAttr("disabled"); 
		$('.btn-oneway').hide(); 
		$('.btn-return').show();
        $('#flight-hotels').hide();
	});
	$('.fl-multicity').on('ifChecked', function (event){
        //alert("hi");
    	$('.srch-oneway-return').hide();
		$('.fh-multicity').show(); 
        $('#flight-hotels').hide();
	});
    $('.fl-flights-hotels').on('ifChecked', function (event){
    	$('.srch-oneway-return').hide();
		$('.fh-multicity').hide();   		
		$('#flight-hotels').show();   		
	});
	$(document).ready(function(){	
	$(".hotel-room-details").each(function(){ 
    var personclone = $(this).find(".cust-row-repeat"), TargetDiv = $(this);
    
		//** Add Button **/
		$(this).find(".add-row-room").click(function(e){
			e.preventDefault();
			
			var TargetElem = TargetDiv.find('.cust-row-repeat'),
				num = TargetElem.length,
				CloneElem = TargetElem.not('.clone-sec').clone(),
				Elemlabel = TargetDiv.find('.lft-lbl-room'),
				ElemLabelCount =  TargetDiv.find('.cust-row').length;

			if(num<5){
				TargetElem.removeClass('ajaxform');
				CloneElem.appendTo(TargetDiv.find('.hotel-room-add')).addClass("clone-sec ajaxform").find('input').not('.spinner input').attr({'id':''}).attr('placeholder');
				$(".clone-sec.ajaxform").find('.spinner input').val('1');
				if(Elemlabel.length){
					TargetDiv.find('.ajaxform').find('.lft-lbl-room').children('span').text((ElemLabelCount+1));
				}
				initAjaxForms();
                
				if(num==4){
					$(this).parent().hide();
					}
				else{
					$(this).parent().show(); 
				  }
				}
			else{
              
                
            }
		});

    	//** Remove Button **/
        $(this).delegate(".del-row-room", 'click', function (e) {
       e.preventDefault();
        
        var TargetElem = TargetDiv.find('.cust-row-repeat'),
             TargetElemRemove = $(this).closest('.clone-sec'),
              numRemove = TargetElem.length;
        
        TargetElemRemove.remove();
        
        console.log(numRemove);
        
         if(numRemove<6){
                TargetDiv.find('.add-row-room').parent().show();
             
         }
        else{
            //alert('bye');
            
        }
             
    });
	});
});
    
/////// 07-06-2017 Flights-hotels Ends : Homepage////////
/*08-06-2017 : Flights : Select Seats*/
$(function(){
    $(document).on('click','.seat-row .seat.available a',function(){
        var el = $(this),el_seat_div = el.parents('.seat.available');
        if(el_seat_div.hasClass('selected'))
        {
            el_seat_div.removeClass('selected');
        }else
        {
            el_seat_div.addClass('selected');
        }

    });
});
/*08-06-2017 : Flights : Select Seats*/
/*08-06-2017 : Flights : Set Flight Alert Popup*/
$('#fixdates').on('ifChecked', function (event){	
$("#fixdates_wrap").show();
$("#flexibledates_wrap").hide();
});
$('#flexibledates').on('ifChecked', function (event){	
$("#fixdates_wrap").hide();
$("#flexibledates_wrap").show();
});
/*08-06-2017 : Flights : Set Flight Alert Popup*/
/*09-06-2017 : Flights : Flight Statuts : Search wrap*/
 $('.radio-departures').on('ifChecked', function (event){
    $('.search-sec #departures').show();		
    $('.search-sec #arrivals').hide();		
});
 $('.radio-arrivals').on('ifChecked', function (event){
    $('.search-sec #departures').hide();		
    $('.search-sec #arrivals').show();		
});
/*09-06-2017 : Flights : Flight Statuts : Search wrap*/

/*19-06-2017 : Hotel : Payment info Equal Heights
if($('.payment-info').length)
    {
        $('.payment-info').parents().find('.pkg-listing').each(function(){  
            var highestBox = 0;
            $(this).find('.payment-info').each(function(){
                if($(this).height() > highestBox){  
                    highestBox = $(this).height();  
                }
            });
            $(this).find('.payment-info').height(highestBox);
        });   
    }
/*19-06-2017 : Hotel : Payment info Equal Heights*/


/*my account*/
$(".btn-edit-profile").click(function(e){
		//alert('Hi');
		$(".profile-brief").hide();
		$(".co-trvl-edit-profile").show();
		$(".accordion-style, .button-right").hide();
		$(".btn-multi-right").show();
	});
	$(".btn-show-trvl").click(function(e){
		$(".profile-brief, .button-right").hide();
		$(".accordion-style").show();
		$(".co-trvl-edit-profile").hide();
	});
	

/*my account*/
/*Login*/
$('.lnk-reset-password').click(function(e){
    e.preventDefault();
    $('.form-cont').hide();
    $('.frgt-pwd-cont').show();
});
$('.lnk-crt-acnt').click(function(e){
    e.preventDefault();
    $('.form-cont').hide();
    $('.crt-acnt-cont').show();
})
$('.lnk-login').click(function(e){
    e.preventDefault();
    $('.form-cont').hide();
    $('.login-cont').show();
});
//		$('.lnk-frgt-pw').click(function(e){
//			e.preventDefault();
//			$('.form-cont').hide();
//			$('.frgt-pwd-cont').show();
//		})
$('.btn-sign-in').click(function(e){
    e.preventDefault();
    resetValidation();
    validateLoginForm();
});
$('.btn-proceed').click(function(e){
    e.preventDefault();			

});		
function validateLoginForm() {
    var empty = 0;
    $('form[name="loginForm"] input').each(function(){
       if (this.value == "") {
           empty++;
           $('.err-msg').show();
           $('.err-msg span').html('Please fix the fields that are marked in Red to continue.');
           $(this).parent().addClass("err");
       } 
    })			   
}		
function resetValidation()
{
    $('input[type=text]').parent().removeClass("err");
    $('.err-msg').hide();
    /*$('.mfp-close').trigger('click');*/
}
/*Login*/
/* 26-06-2017 View More*/
$(".btn-view-more").click(function(e){
	e.preventDefault();
	if(!$(this).hasClass('active')){
		$(this).text('View Less');
        $(this).closest('div').find(".collapse-content").slideDown();
        $(this).addClass('active');
	}
	else{
		$(this).text('View More');
		$(this).closest('div').find(".collapse-content").slideUp();
		$(this).removeClass('active');
	}
});
/* 26-06-2017 View More*/
/* 26-06-2017 More Details*/
$(document).delegate('.more-deals','click', function(e) {
    
    $(this).closest(".pkg-listing > li").find(".pkg-similar-list").slideDown();
    $(this).closest(".pkg-listing > li").find(".pkg-similar-head").addClass("active");
    $('html, body').animate({ scrollTop: $(this).closest(".pkg-listing > li").find(".pkg-similar").offset().top-150 }, 300);
e.preventDefault();
});
/* 26-06-2017 More Details*/
/*28-06-2017 - Holiday - Group*/
$(document).delegate('.hub-edit-lbl','click', function(e) {
    e.preventDefault();
    //alert('hi');
    $(this).parent().find('.hub-city-list').toggle();
});
$(document).delegate('.hub-city-list > ul > li > a','click', function(e) {
    e.preventDefault();
    $(this).parent().find('.hub-city-list').toggle();
});
/*28-06-2017 - Holiday - Group*/
// 29-06-2017 INSURANCE

$('.single-trip').on('ifChecked', function (event){
    $('#single-trip').show();
    $('#multi-trip').hide();
});

$('.multi-trip').on('ifChecked', function (event){
    $('#single-trip').hide();
    $('#multi-trip').show();
});

// 29-06-2017 INSURANCE :: Ends

/////// 30-06-2017 Bus Start : Homepage////////
	$('.bus-one-way').on('ifChecked', function (event){		
    	$('.srch-oneway-return').show();
		$('.fh-multicity').hide();   
		$('.cf-return').addClass("disabled");
		$(".cf-return input").attr("disabled","disabled");
		$('.btn-oneway').show(); 
		$('.btn-return').hide();
        $('#bus-package').hide();
	});
	$('.bus-return').on('ifChecked', function (event){			
    	$('.srch-oneway-return').show();
		$('.fh-multicity').hide();   
		$('.cf-return').removeClass("disabled");
        $(".cf-return input").removeAttr("disabled"); 
		$('.btn-oneway').hide(); 
		$('.btn-return').show();
        $('#bus-package').hide();
	});
	$('.bus-pkg').on('ifChecked', function (event){
    	$('.srch-oneway-return').hide();
		$('.fh-multicity').hide();   		
		$('#bus-package').show();   		
	});
/////// 30-06-2017 Bus Ends : Homepage////////

/*Login Page - Sign In*/
$(".btn-sign-in").click(function(){
   $("header .top-bar .top-bar-rt > ul > li.after-lgn").hide(); 
   $("header .top-bar .top-bar-rt > ul > li.before-lgn").css("display","inline-block"); 
});
/*Login Page - Sign In*/

/*
$('.checkbox-container, .checkbox-container .iCheck-helper').click(function(){
	$('.password').toggle();
})
*/



$('.filter-search-panel-toggle').click(function(e) {
	$(this).toggleClass('active');
    $('.tab2-sec .resp-tabs-list').slideToggle();
});

$(".TimeCarousel").mCustomScrollbar({
    scrollButtons:{enable:true},
    theme:"light-thick",
    scrollbarPosition:"outside"
}); 

$(".depar-arriv-sec > div").click(function(e){
    e.preventDefault();
     $(this).siblings().removeClass('active');
    $(this).addClass('active');
});


function checkWidth() {
    if ($(window).width() < 599) {
        $('.pkg-list.listing .pkg-listing').addClass('grid-view');
        $('.pkg-list.listing .pkg-listing').removeClass('list-view');
    } 
}

$(window).resize(checkWidth);
                                  
/*$('#Horizontal_Tab h2[aria-controls="hor_2_tab_item-2"]').click(); */
$(document).on("click",".cars .transfer-details a.recomended-btn", function(e) {
    if(!$(this).hasClass('active')){
        $(this).removeClass("active");
    $(this).parents('li').find('.rcmd-extrs').slideDown(); 
    $(this).parents('li').find('.rcmd-extrs').find('input[type=checkbox]').each(function(){
        //$(this).unwrap('.styled-checkbox');
    $(this).parent().find('.iCheck-helper, .check').remove();
    $(this).iCheck({'checkboxClass': 'styled-checkbox','checkedClass': 	'styled-checkbox-checked','insert': '<div class="check"></div>'});
        if($(this).parents('.chk-bx').find('.styled-checkbox .styled-checkbox').length){
        $(this).unwrap('.styled-checkbox');
    }
    })
    }
    else{
        $(this).addClass("active");
        //$(this).parents('li').find('.rcmd-extrs').find('input[type=checkbox]').wrap('.styled-checkbox');
        $(this).parents('li').find('.rcmd-extrs').slideUp(); 
    }
    $(this).toggleClass("active");    
    e.preventDefault();
});


/*Collection - Explore Brochure Online*/
$('.explore-brochure .four-item-carousel').owlCarousel({
   items:4, loop:false,margin:32,nav:true,navText:[ , ],mouseDrag: false,pullDrag: false,responsive:{0:{items:1},600:{items:2},992:{items:3},1200:{items:4}}
}); 
/*Collection - Explore Brochure Online*/
/*Gift Cards - All Cards*/
$(".cls-btn").click(function(){
    $(".extend-validity").hide();
    $(".extend-validity-btn").removeClass('active');
});
    
$(".extend-validity-btn").click(function(){
    $(".extend-validity").show();
    $(this).addClass('active');
});

$(".cls-btn").click(function(){
    $(".top-up").hide();
    $(".top-up-btn").removeClass('active');
});
    
$(".top-up-btn").click(function(){
    $(".top-up").show();
    $(this).addClass('active');
});

$(".cls-btn").click(function(){
    $(".report").hide();
    $(".report-btn").removeClass('active');
});
    
$(".report-btn").click(function(){
    $(".report").show();
    $(this).addClass('active');
});

$(".get-details").click(function(e){
    e.preventDefault();
    $(".check-balance-cnt").hide();
    $(".pre-paid-cnt").show();
});

$(".extend-validity-btn").click(function(){
$(".buy-gift-card-sec").show();
$(this).addClass('active');
});
/*Gift Cards - All Cards*/

/*Cruise Ammendment and cancellation - add-product-search-result-flight*/
//Tooltip Details starts
$(document).on("click",".details-btn", function(e) {

    e.preventDefault();
	
   if($(this).hasClass("active"))
       {           
            
        $(this).parents('.result-item').find('.full-width').hide();
        $(this).parents('.result-item').find('a.details-btn, .btn-pick-seat').removeClass("active");
          
           
           if($('.grid-view').length)
           {               
                $(this).parents('.result-item').find('.detail-container').slideUp({
                   complete: function(){
                            $(this).parents('.pkg-listing').css("height","auto");
                            $('.grid-view .result-item').css("height","auto");
                        }
                    });              
           }
           else{
                   $(this).parents('.result-item').find('.detail-container').slideUp(); 
               }
           
       }
   else{
        
              
               $('.detail-container.active').slideDown(
                    {
                   complete: function()
                        {
                            $(this).parents('.pkg-listing').css("height","auto");
                            $('.grid-view .result-item').css("height","auto");
                            //$(this).parents('.result-item').height(dtlHeight);
                        }
                    }
                    );
               $('.detail-container').removeClass("active");
               $('.btn-details').removeClass("active");
               $(this).addClass("active");
               $('.pick-seats-container').hide();
               $(this).parents('.result-item').find('.detail-container').addClass("active");
               $(".tool-tip-pick-seats > a").removeClass('active');
       
       if($('.grid-view').length)
       {
          
            $(this).parents('.result-item').find('.detail-container').slideDown(
                {
                    complete: function()
                                    {
                                        elemHeight= $(this).parents('.result-item').height();
                                        blockHeight=$(this).parents('.result-item').find('.detail-container').height();
                                        dtlHeight=elemHeight+blockHeight;
                                        $(this).parents('.pkg-listing').css("height","auto");
                                        $('.grid-view .result-item').height(elemHeight);
                                        $(this).parents('.result-item').height(dtlHeight);
                                        $(".grid-view > li:nth-child(4)").css("clear","both");
                                       
                                    }
                }
            );
           
        }
        else
            {
                 $(this).parents('.result-item').find('.detail-container').slideDown();
            }
    }
    

    $(this).parents().find('.result-item').css({ zIndex: 11111});
    $('.rsltDtlTab.HrzTab').responsiveTabs('activate', 0);

});
//Tooltip Details Ends
	

$(".full-width .btn-close").click(function(e){
    $(this).parent().slideUp();
    $(this).parents('.result-item').find('a.details-btn').removeClass("active");
    $(this).parents('.result-item').find('.btn-pick-seat').removeClass("active");
     $(this).parents('.grid-view').css("height","auto");
    //$(this).parents('.grid-view .result-item').height(elemHeight);
    $(this).parents('.result-item').css("height","auto");
});

$('.btn-close').click(function(e) {
	$(this).parent('.share-collapse').hide();
	e.preventDefault();
    $(".share-tooltip a").removeClass("active");
    $.magnificPopup.close();
});
	
/*Flight Result togle tab Start*/
$("#listview-flight").click(function(){
    $("#listview-details").show();
	$(".viewswitcher").removeClass('active');
    $("#calendar-view-details, #grid-view-details").hide();
	$("#listview-flight").addClass('active');
});
$("#calenderview-flight").click(function(){
    $("#calendar-view-details").show();
    $("#listview-details, #grid-view-details").hide();
	$(".viewswitcher").removeClass('active');
	$("#calenderview-flight").addClass('active');
});
$("#gridview-flight").click(function(){
    $("#grid-view-details").show();
    $("#listview-details, #calendar-view-details").hide();
	$(".viewswitcher").removeClass('active');
	$("#gridview-flight").addClass('active');
});
/*Flight Result togle tab End*/
	$(".train-details").click(function(e){
        $(this).toggleClass("active");
        var detaiButtnPos=$(this).offset().top;
        $(this).closest('.custm-tbl > .common-tbl-row').find(".train-summary").slideToggle();
        //$(".train-summary").show(); 
        $(".train-summary").offset({ top: detaiButtnPos+$(this).outerHeight()});
        $(this).parents().find(".train-item").css({ zIndex: 11111});
    });
    
    $('.flights .srch-result .travel-detail-container .close-btn-new').click(function(){
        $(this).parents('.train-summary').slideUp();
         $(this).parents('.train-summary').parent('.common-tbl-row').find('.train-details').removeClass('active');
        return false;
    });

$('.ul-sort.border-list li').click(function() {
    $(this).addClass('active').siblings().removeClass('active');
});

$(".check-avail").click(function(e) {
	e.preventDefault();
	$(this).closest('.list-view.pkg-listing > li').find(".chk-availabilty").slideToggle();
    $(this).toggleClass("active");
	$('.travel-detail-container').hide();
});

$(".chk-availabilty .btn-close").click(function(){
    $(".chk-availabilty").hide();
    $(".check-avail").removeClass('active');
});

/*var container = $(".train-summary");    
if (!container.is(e.target) 
    && container.has(e.target).length === 0) 
{
    $(".train-details").removeClass("active");
    container.hide();
}*/
/*Cruise Ammendment and cancellation - add-product-search-result-flight.*/
/*Flights - Flights Result Domestic Multicity*/
$('.flights .price-diffrence-right .btn').click(function(){			
    $('.price-diffrence-right .btn').html('select').removeClass('selected');
    $(this).html('selected').addClass('selected');
    $('.packag-sec-cont.sec2').show();
    $('.packag-sec-cont.sec2.empty').hide();
});
/*Flights - Flights Result Domestic Multicity*/
/*Destinations - Destinations Goa Cuisine*/
$(".mre-lists .btn-close").click(function(e){
    $(this).closest('.collapse-block').children('.collapse-container').slideToggle();
});	
/*Destinations - Destinations Goa Cuisine*/

$(".sort-sec .filter-search-panel .sort-option-list a").click(function(e) {
	e.preventDefault();
    $(this).parents('.sort-option-list').find('li').removeClass('active');
    $(this).parent().addClass('active');
    $(this).toggleClass('dsc')
    $(this).toggleClass('asc')
});

/*Flights Search Widget Multi Calendar*/
$('.depart-cal input').click(function(){
    if(($('input[name=fromdestination]').val() == '')&&($('input[name=todestination]').val() == '')){
        //alert('Price will not show up');
        $('.custom-calender .custom-multi-calendar').removeClass("CalendarwithPrice");
       $(".form-des-details.custom-calender").hide();
    }
    else{
       //alert('Price will not show up for '+ $('input[name=fromdestination]').val() + ' and ' +  $('input[name=todestination]').val());
        $('.custom-calender .custom-multi-calendar').addClass("CalendarwithPrice");
        $(".form-des-details.custom-calender").show();
        
    }
 });
/*Flights Search Widget Multi Calendar*/
/*Flights - Index */
$('.chk-date-diff').click(function(){
		$('.fh-destination-cont').toggle();
	});
/*Flights - Index */


$(".find-store .left-panel figure").click(function(){
    $(".map-office-popup").show();
});


$(".map-office-popup .mfp-close").click(function(){
	$('.map-office-popup').hide();
});

/***** Checkout Login Starts *****/
$('#create-acnt').click(function(){
    $('.signin-box').hide();
    $('.signup-box').show();
});
$('#signup-back').click(function(){
    $('.signin-box').show();
    $('.signup-box').hide();
});

$('.create-accnt').on('click', function(){
		$('.signup-box').hide();
		$('.accnt-created').show();
	})

$('.lnk-frgt-pw').click(function(){
    $('.signin-form > div').hide();
    $('.forgot-sec').show();		
})
$('.btn-frgt-submit').click(function(){
    $('.signin-form > div').hide();
    $('.forgot-success-sec').show();		
})
$('.lnk-reset-pw').click(function(){
    $('.signin-form > div').hide();
    $('.reset-pw-sec').show();		
})
$('.btn-new-login').click(function(){
    $('.signin-form > div').hide();
    $('.new-login-sec').show();
})    
/***** Checkout Login Ends *****/

/*** Pakage Bar Fixed After Scroll ***/
var initFixedPkgBar = function() {
   
var fixedmidsec   = $('.top-mid-sec'),
    DivOffset     = fixedmidsec.offset().top-$('header').outerHeight();
//alert(DivOffset); 
$(window).scroll(function(){
	if ($(this).scrollTop()>DivOffset){
		$('.pkg-bar').addClass('fixedpos');
	} 
	else{
		$('.pkg-bar').removeClass('fixedpos');
	}
});
}
$('.seltaddr').on('ifChecked', function(){
        if ($("#drop_addrs").is(":checked")) {
            $('.drop_addrs').show();
			$('.others_addrs').hide();
        }
       else if ($("#others_addrs").is(":checked")) {
		    $('.drop_addrs').hide();
            $('.others_addrs').show();
        }
	  else if ($("#pickup_addrs").is(":checked")) {
		    $('.drop_addrs').hide();
            $('.others_addrs').hide();
        }
 });
//$('.seltaddr').on('ifChecked', function(){
//        if ($("#others_addrs").is(":checked")) {
//            $('.residential-address').show();
//        }
//        else 
//            $('.residential-address').hide();
//                                });

//////******* Bus Flow Starts *******//////

$(function(){

    $('.rsltDtlTab.HrzTab').responsiveTabs();
        
    $(document).on('click','.bus .seat-row > .seat.available a',function(){
        var el = $(this),el_seat_div = el.parents('.seat.available');
        if(el_seat_div.hasClass('selected'))
        {
            el_seat_div.removeClass('selected');
        }else
        {
            el_seat_div.addClass('selected');
        }
        
        $(this).parents('.result-item').find('.slct-brdng-pnt').show();
        $(this).parents('.result-item').find('.bording-point').hide(); 

    });
    
    $('.fare-details input[name="boarding-point"]').on('ifClicked', function (event) {
        
        $(this).parents('.result-item').find('.slct-brdng-pnt').hide();
        $(this).parents('.result-item').find('.bording-point').show(); 
    });
   
   
    
    $('.bus .srch-result .fare-details .clpsbl-link > a').click(function(e){
        e.preventDefault();
        $('.slct-brdng-pnt').show();
        $('.bording-point').hide();    
    });
    
});
  
$(".btn-pick-seat").click(function(e){
    
    e.preventDefault();
	
    if($(this).hasClass("active"))
    {           
         $('.btn-pick-seat').removeClass("active");
        $(this).parents('.result-item').find('.full-width').hide();
        $(this).parents('.result-item').find('a.details-btn').removeClass("active");
        $(this).parents('.result-item').find('.btn-pick-seat').removeClass("active");
           
            
        if($('.grid-view').length)
        {
                
            $(this).parents('.result-item').find('.pick-seats-container').slideUp(
            {
                complete: function()
                 {
                     $(this).parents('.pkg-listing').css("height","auto");
                     $('.grid-view .result-item').css("height","auto");
                 }
             });
               
        }
        else
        {
            $(this).parents('.result-item').find('.pick-seats-container').slideUp(); 
        }
            
    }
    else{
        $('.btn-pick-seat').removeClass("active");
        $(this).parents('.result-item').find('.full-width').hide();
        $(this).parents('.result-item').find('a.details-btn, .btn-pick-seat').removeClass("active");
        
        $('.pick-seats-container.active').slideUp(
        {
            complete: function()
            {
                $(this).parents('.pkg-listing').css("height","auto");
                $('.grid-view .result-item').css("height","auto");
                //$(this).parents('.result-item').height(dtlHeight);
            }
        });
        $('.pick-seats-container').removeClass("active");
        $('.btn-details').removeClass("active");
        $(this).addClass("active");
        $('.pick-seats-container').hide();
        $(this).parents('.result-item').find('.pick-seats-container').addClass("active");
        $(".tool-tip-pick-seats > a").removeClass('active');
        
        if($('.grid-view').length)
        {
           
            $(this).parents('.result-item').find('.pick-seats-container').slideDown(
            {
                complete: function()
                {
                    elemHeight= $(this).parents('.result-item').height();
                    blockHeight=$(this).parents('.result-item').find('.pick-seats-container').height();
                    dtlHeight=elemHeight+blockHeight;
                    $(this).parents('.pkg-listing').css("height","auto");
                    $('.grid-view .result-item').height(elemHeight);
                    $(this).parents('.result-item').height(dtlHeight);
                    $(".grid-view > li:nth-child(4)").css("clear","both");
               
                }
            });
           
        }
        else{
            $(this).parents('.result-item').find('.pick-seats-container').slideDown();
        }
    }
    
    $(this).parents().find('.result-item').css({ zIndex: 11111});
    $('.rsltDtlTab.HrzTab').responsiveTabs('activate', 0);

});

//////******* Bus Flow Ends *******//////


$('#saved-traveler input').on('ifChecked', function(){
    $(this).closest("form").find(".btn-cont .btn-close").removeClass("disabled");
    $(".pop-tl").hide();
    $(".saved-msg").show();
});

$('#saved-traveler input, #car-saved-traveler input').on('ifChecked', function(){
    $(this).closest("form").find(".btn-cont .btn-close").removeClass("disabled");
    $(".pop-tl").hide();
    $(".saved-msg").show();
});


$(document).on('ifChecked','.pkg-similar-list input',function(){
    $(this).closest("li").addClass("selected");
});
$(document).on('ifUnchecked','.pkg-similar-list input',function(){
    $(this).closest("li").removeClass("selected");
});

/*23Aug2017*/

/*05-09-2017 Multicity search panel*/
$(".multi-input").click(function(){
        $(this).parent().parent().hide();
        $(".modify-search").show();
    })
/*05-09-2017 Multicity search panel*/


/*Flight Status - By Airport - 11/09/2017*/
$('.flight-departure').on('ifChecked', function (event){		
    	$('#flight-departure').show();
    	$('#flight-arrivals').hide();
});
$('.flight-arrivals').on('ifChecked', function (event){		
    	$('#flight-departure').hide();
    	$('#flight-arrivals').show();
});
/*Flight Status - By Airport - 11/09/2017*/

/* Set Equal Height --> Flight Return : starts */
var pkg_thumbnail_height_set_val = 0;
$('.flights .flight-return-sel .result-item').each(function() {
    var pkg_thumbnail_height = parseInt($(this).find('.pkg-thumbnail').css('height').slice(0, -2));
    pkg_thumbnail_height_set_val = (pkg_thumbnail_height_set_val < pkg_thumbnail_height) ? pkg_thumbnail_height : pkg_thumbnail_height_set_val;
});

$('.flights .flight-return-sel .result-item .pkg-thumbnail.type-03').css({
    'height': pkg_thumbnail_height_set_val + 'px'
});
/* Set Equal Height --> Flight Return : ends */

/* Checkout - Rail Starts*/

	   $('#nationality ').on('change', function() {
      if ( this.value !== '1')
      {
        $(".id-typ-shw").show();
      }
      else
      {
        $(".id-typ-shw").hide();
      }
      });
	  $(".add-morebtn").on('click',function(e){
		  e.preventDefault();
		  $(this).parents('dd').find(".duplicate-rw").show();
	  })
	  $('.minus-btn').on('click', function(e){
		  e.preventDefault();
		  $(this).parents('dd').find(".duplicate-rw").hide(); 
	   })
	  
  
/* Checkout - Rail Ends*/


// Rail-Worldwide      
$('.rw-one-way').on('ifChecked', function (event){
    $('#rw-oneway').show();
    $('#rw-return').hide();
    $('#rw-multicity').hide();
})  
$('.rw-return').on('ifChecked', function (event){
    $('#rw-oneway').hide();
    $('#rw-return').show();
    $('#rw-multicity').hide();
})  
$('.rw-multicity').on('ifChecked', function (event){
    $('#rw-oneway').hide();
    $('#rw-return').hide();
    $('#rw-multicity').show();
})

// Seat Reservation
$('.sr-one-way').on('ifChecked', function (event){
    $('#sr-oneway').show();
    $('#sr-return').hide();
    $('#sr-multicity').hide();
})  
$('.sr-return').on('ifChecked', function (event){
    $('#sr-oneway').hide();
    $('#sr-return').show();
    $('#sr-multicity').hide();
})  
$('.sr-multicity').on('ifChecked', function (event){
    $('#sr-oneway').hide();
    $('#sr-return').hide();
    $('#sr-multicity').show();
});
$('.print-paper-ticket').on('ifChecked', function (event){
    $('#print-paper-ticket').show();
})
$('.print-home').on('ifChecked', function (event){
    $('#print-paper-ticket').hide();
})
$('.print-ticket-stn').on('ifChecked', function (event){
    $('#print-paper-ticket').hide();
})

// Seat Reservation
$('.custom-info-input > .holder').click(function(){
    $(this).fadeToggle();
});

$('#country-input').keyup(function(){
    var input = $(this).val();
    if(input == 'Japan'){
        $("#all-country").hide();
         $("#japan-country").show();
    }
    else {
        $("#all-country").show();
    }
});

$('.image-popup').magnificPopup({
    type: 'image',
    closeOnContentClick: true,
    mainClass: 'mfp-img-mobile',
    image: {
    verticalFit: true
    }

});

//$(document).on("click",".btn-view-details", function(e) {
//e.preventDefault();
//
//var elDetailContainer = $(this).parents('.result-item').find('.detail-container');
//if(elDetailContainer.css('display') == 'none'){	
//    elDetailContainer.slideDown().addClass("active");
//}else{
//    elDetailContainer.slideUp().removeClass("active");
//}
//
//});
$('input[name=upgrade-ticket]:radio').on('ifChecked', function(event){

 var resultnfare_details = $(this).parents('.resultnfare-details');
    resultnfare_details.find('.fare-brkup > .upgrade').css({'display':'inline-block'});
    resultnfare_details.find('.fare-ttl .fare-rslt').html('<i class="fa-inr"></i> 25,854')

});
$(document).on("click",".btn-train-details", function(e) {
    e.preventDefault();
    var elDetailContainer = $(this).parents('.result-item').find('.train-detail-container');
if(elDetailContainer.css('display') == 'none'){	
    elDetailContainer.slideDown().addClass("active");
}else{
    elDetailContainer.slideUp().removeClass("active");
}
//.slideDown()
});
// Rail-Worldwide

/*30-10-2017 - Holiday Details BD*/
$('.cat-economy').on('ifChecked', function (event){
   $(this).closest('.price-table-sec').find('.economy').show(); 
   $(this).closest('.price-table-sec').find('.standard').hide();  
   $(this).closest('.price-table-sec').find('.deluxe').hide(); 
   $(this).closest('.price-table-sec').find('.premium').hide();     
});
$('.cat-standard').on('ifChecked', function (event){
   $(this).closest('.price-table-sec').find('.economy').hide(); 
   $(this).closest('.price-table-sec').find('.standard').show();  
   $(this).closest('.price-table-sec').find('.deluxe').hide(); 
   $(this).closest('.price-table-sec').find('.premium').hide();  
})
 $('.cat-deluxe').on('ifChecked', function (event){
   $(this).closest('.price-table-sec').find('.economy').hide(); 
   $(this).closest('.price-table-sec').find('.standard').hide();  
   $(this).closest('.price-table-sec').find('.deluxe').show(); 
   $(this).closest('.price-table-sec').find('.premium').hide();  
})
 $('.cat-premium').on('ifChecked', function (event){
   $(this).closest('.price-table-sec').find('.economy').hide(); 
   $(this).closest('.price-table-sec').find('.standard').hide();  
   $(this).closest('.price-table-sec').find('.deluxe').hide(); 
   $(this).closest('.price-table-sec').find('.premium').show();  
})
/*30-10-2017 - Holiday Details BD*/

/* Autocomplete Add : 02-11-2017 : Start */
$(function(){

    var availableTags = ["Himachal Pradesh","Himachal City","Himachal Park"];       
    //Add Function
    $('.autocomplete-add input[name="search"]').autocomplete({
        source: availableTags,
        select: function (event, ui) {        

            //console.log(ui.item.value);
            var el = $(this),
                el_selectData = el.parents('.autocomplete-add').find('.select-data'),
                blAdd = true;
            
            el_selectData.find('span').each(function(){

                if($(this).attr('data-label') == ui.item.value && blAdd)
                {
                    blAdd = false;
                    return false;
                }
                
            });

            if(blAdd)
            el_selectData.append('<span data-label="'+ ui.item.value +'">'+ ui.item.value +' <a class="rmv"><i class="fa-times-circle"></i></a></span>');

            el.val('');
            
            return false;
        }
    });	        
    //Remove Function
    $(document).on('click','.autocomplete-add > .select-data a.rmv',function(){
        $(this).parents('span').remove();
        $(this).parents('.autocomplete-add').find('[name="search"]').val('');
    });

	
	//var autoCities = ["European Gems","European Summer","Europe Adventures","Europe City","Europe Discovery","Europe Lifestyle"];
	var autoCities = ["European Gems","European Summer","Europe Adventures","Europe City","Europe Discovery","Europe Lifestyle","Oriental Gateway","Twin Treats","Oriental Gateway","Oriental Magic","Oriental Gateway","Oriental Magic"];
	$( ".auto-cities" ).autocomplete({source: autoCities});
	$('.add-co-trvlr').click(function(e){
		e.preventDefault();
		$(".co-trvlr-blk").toggle();	
		$(this).text($(this).text() == '- Add Co-Traveler' ? '+ Add Co-Traveler' : '- Add Co-Traveler');
 	});
	
	$('.search-form [name="search"]').click(function(){
		var el = $(this),
			has_feedback = el.parents('.has-feedback');
			if(!has_feedback.hasClass('active'))
				has_feedback.addClass('active');        
	}).blur(function(){        
		 var el = $(this),
			has_feedback = el.parents('.has-feedback');
		if(el.val().trim() == '')
			has_feedback.removeClass('active');
	});
	
});
/* Autocomplete Add : 02-11-2017 : End */