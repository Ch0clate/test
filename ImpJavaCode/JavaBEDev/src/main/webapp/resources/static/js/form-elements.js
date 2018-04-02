/* Initialisers
-------------------------------*/
forms = $('form, .form');
isIE7 = $('html').hasClass('ie7');
isIE8 = $('html').hasClass('ie8');
touchEnabled = Modernizr.touch;		
formPlaceholders = Modernizr.input.placeholder;
boxShadows = Modernizr.boxshadow;


	//////////////////////
	// initalises forms, standardises cross-device behaviours
	//////////////////////
	function initForms(){
		forms.each(function(){
			var $this = $(this),
				allInputs = $this.find('input'),
				allSelects = $this.find('select'),
				placeholderInputs = $this.find('[placeholder]'),
				checkboxInputs = $this.find('input[type="checkbox"]'),
				radioInputs = $this.find('input[type="radio"]'),
				coupledCheckboxes = $this.find('.coupled-checks');
			
			// apply custom iCheck styles to checkboxes
			checkboxInputs.iCheck({
				'checkboxClass': 	'styled-checkbox',
				'checkedClass': 	'styled-checkbox-checked',
				'insert':			'<div class="check"></div>'
			});

			// apply custom iCheck styles to radios
			radioInputs.iCheck({
				'radioClass':		'styled-radio',
				'checkedClass': 	'styled-radio-checked',
				'insert':			'<div class="check"></div>'
			});
			
			// ensure all forms are reset
			if (this.hasClass = "form"){
			}
			else{
			this.reset();
			}

			 // loop over each input
			 allInputs.each(function(){
				var $this = $(this),
					type = $this.attr('type');
				
				if(!boxShadows){
					$this.addClass(type+'-input');	
				}
			 });

			 // loop over each select to build custom select styles
			 allSelects.each(function(){
				
				// not for IE7
				if(!isIE7){
					var $this = $(this),
						wrap = $('<div />', {
							'class':	'styled-select'
						});
						
					// wrap
					$this.wrap(wrap);
	
					// invoke bootstrap-select plugin
					
					$this.selectpicker({
						'showIcon':	true,
						'dropupAuto':	false,
						'showSubtext' : true,
						'showContent' : true
					});
					
					if (window.PIE) {$('.form-group .styled-select .btn-group .btn').each(function() {PIE.attach(this);});} 	
				}
			 });

			// if no native support for placeholders
			if(!formPlaceholders){
				// loop through each
				placeholderInputs.each(function(){
					var $this = $(this),
						placeholderClass = 'placeholder',
						thisText = $this.val(),
						placeholderText = $this.attr('placeholder');
	
					// initialisation
					if(thisText !== placeholderText){
						//add placeholder classname and set value to placeholder text
						if(!$this.hasClass('password-input')){
						   $this.addClass(placeholderClass).val(placeholderText);
						   
						   }
						else{
						   //alert($this.attr('type'))
						   $this.wrap( "<div></div>" );
						   $this.addClass('real-pass').val('').parent().addClass('ie8-pass-div').append('<input type="text" class="dummy-pass" value="'+placeholderText+'"/>');
						   
						}
					}
					
					//focus
					$this.not(".real-pass, .dummy-pass").focus(function(){
						if($this.val() == $this.attr('placeholder')){
						   if(!$this.hasClass('password-input')){
							$this.removeClass(placeholderClass).val('');
							}	
						}
					});
					
					//blur
					$this.not(".real-pass, .dummy-pass").blur(function(){
						if($this.val() === '' || $this.val() == $this.attr('placeholder')){
						  if(!$this.hasClass('password-input')){
							//add placeholder classname and set value to placeholder text
							$this.addClass(placeholderClass).val(placeholderText);
							}
						}
					})
					.blur();
				});
			}
			
			// coupled checkboxes
			if(coupledCheckboxes.length){
				coupledCheckboxes.each(function(){
					var checkboxes = $(this).find('input[type=checkbox]');
	
					// loop over each checkbox
					checkboxes.each(function(){
						// identify other checkboxes in this set
						var otherChecks = checkboxes.not($(this));
						
						// tie into iCheck 'change' event
						$(this).on('ifChanged', function(){
							// if this is now selected
							if($(this).attr('checked') == 'checked'){
								otherChecks.iCheck('uncheck');
							}
						});
					});
				});
			}
			
		});
		
		//Customisation Of Onselect, OnChecked
        $(document).on("ifChecked",".radio-inline input", function(e) {$(this).closest('label').addClass('selected');});
        $(document).on("ifUnchecked",".radio-inline input", function(e) {$(this).closest('label').removeClass('selected');});
        //$('.radio-inline input').on('ifChecked', function(){$(this).closest('label').addClass('selected');});
        //$('.radio-inline input').on('ifUnchecked', function(){$(this).closest('label').removeClass('selected');});
        
		$('.specific-time').on('ifChanged', function(){ $('#contactDateDiv').slideToggle();});
		$('.rooming-same-select').on('ifChanged', function(){$('.rooming-same').toggleClass('select');});
		$('.room-select').on('ifChanged', function(){$(this).parent().parent().parent().toggleClass('selected');});
		//$('.radio-inline input').on('ifChanged', function(){$(this).parent().parent().toggleClass('selected');});
		
			$('.online').on('ifChecked', function(){$('.payment-mode').show();$('#offline').fadeOut(function(){$('#online').fadeIn()});$('.book-ext-sect li:first-child').removeClass('last');});
		$('.offline').on('ifChecked', function(){$('.payment-mode').show();$('#online').fadeOut(function(){$('#offline').fadeIn()});$('.book-ext-sect li:first-child').removeClass('last');});
		/** Bokig Summary ***/
		$('.PrintAll').on('ifChecked', function(){ $(".Print").iCheck('check').iCheck('disable'); $(".print-check-row").addClass('disabled');});
        $('.checkAll').on('ifChecked', function(){ $(".check").iCheck('check').iCheck('disable'); $(".print-check-row").addClass('disabled');});
		$('.EmailAll').on('ifChecked', function(){ $(".Email").iCheck('check').iCheck('disable'); $(".email-check-row").addClass('disabled');});
		$('.PrintAll').on('ifUnchecked', function(){ $(".Print").iCheck('uncheck').iCheck('enable'); $(".print-check-row").removeClass('disabled');});
        $('.checkAll').on('ifUnchecked', function(){ $(".check").iCheck('uncheck').iCheck('enable'); $(".print-check-row").removeClass('disabled');});
		$('.EmailAll').on('ifUnchecked', function(){ $(".Email").iCheck('uncheck').iCheck('enable'); $(".email-check-row").removeClass('disabled');});
		
		$('.CheckAll').on('ifChecked', function(){ $(this).closest('.pax-tbl').find(".Check").iCheck('check').iCheck('disable'); $(this).closest('.pax-tbl').find(".check-row").addClass('disabled');});
		$('.CheckAll').on('ifUnchecked', function(){$(this).closest('.pax-tbl').find(".Check").iCheck('uncheck').iCheck('enable'); $(this).closest('.pax-tbl').find(".check-row").removeClass('disabled');});
        		$('.radio-inline input').on('ifUnchecked', function(event){	
            $(this).closest('.radio-inline').removeClass('active selected');
        });
        $('.checkbox-inline input').on('ifChecked', function(event){
            $(this).closest('.checkbox-inline').addClass('active selected');
        });
        
		$('.checkbox-inline input').on('ifUnchecked', function(event){	
            $(this).closest('.checkbox-inline').removeClass('active selected');
        });
		/***/
		
	   
		 $('.checkbox-container').find('input[type=checkbox]').on('ifChecked', function(){
            $(".pw-froup").show();
            $(".btn-login").show();
            $(".btn-login-cont").hide();
         }).on('ifUnchecked', function(){
            $(".pw-froup").hide();
            $(".btn-login").hide();
            $(".btn-login-cont").show();
         });
		
	}

/**********Custom Date Picker birthdate********/


initForms();


/* Ajax based Forms Initialisers
-------------------------------*/
AjaxForm = $('.ajaxform');

	//////////////////////
	// initalises forms, standardises cross-device behaviours
	//////////////////////
	function initAjaxForms(){
		AjaxForm.each(function(){
			var $this = $(this),
				allInputs = $this.find('input'),
				allSelects = $this.find('select'),
				placeholderInputs = $this.find('[placeholder]'),
				checkboxInputs = $this.find('input[type="checkbox"]'),
				radioInputs = $this.find('input[type="radio"]'),
				coupledCheckboxes = $this.find('.coupled-checks');
			
			// apply custom iCheck styles to checkboxes
			checkboxInputs.iCheck({
				'checkboxClass': 	'styled-checkbox',
				'checkedClass': 	'styled-checkbox-checked',
				'insert':			'<div class="check"></div>'
			});

			// apply custom iCheck styles to radios
			radioInputs.iCheck({
				'radioClass':		'styled-radio',
				'checkedClass': 	'styled-radio-checked',
				'insert':			'<div class="check"></div>'
			});
			
			// ensure all forms are reset
			if (this.hasClass = "form"){
			}
			else{
			this.reset();
			}

			 // loop over each input
			 allInputs.each(function(){
				var $this = $(this),
					type = $this.attr('type');
				
				if(!boxShadows){
					$this.addClass(type+'-input');	
				}
			 });

			 // loop over each select to build custom select styles
			 allSelects.each(function(){
				
				// not for IE7
				if(!isIE7){
					var $this = $(this),
						wrap = $('<div />', {
							'class':	'styled-select'
						});
						
					// wrap
					$this.wrap(wrap);
	
					// invoke bootstrap-select plugin
					
					$this.selectpicker({
						'showIcon':	true,
						'dropupAuto':	false,
						'showSubtext' : true,
						'showContent' : true
					});
					
					if (window.PIE) {$('.form-group .styled-select .btn-group .btn').each(function() {PIE.attach(this);});} 	
				}
			 });

			// if no native support for placeholders
			if(!formPlaceholders){
				// loop through each
				placeholderInputs.each(function(){
					var $this = $(this),
						placeholderClass = 'placeholder',
						thisText = $this.val(),
						placeholderText = $this.attr('placeholder');
	
					if(thisText !== placeholderText){
						if(!$this.hasClass('password-input')){
						   $this.addClass(placeholderClass).val(placeholderText);
						   
						   }
						else{
						   //alert($this.attr('type'))
						   $this.wrap( "<div></div>" );
						   $this.addClass('real-pass').val('').parent().addClass('ie8-pass-div').append('<input type="text" class="dummy-pass" value="'+placeholderText+'"/>');
						   
						}
					}
					
					//focus
					$this.not('.real-pass, .dummy-pass').focus(function(){
						if($this.val() == $this.attr('placeholder')){
						   if(!$this.hasClass('password-input')){
							$this.removeClass(placeholderClass).val('');
							}
						}
					});
					
					//blur
					$this.not('.real-pass, .dummy-pass').blur(function(){
						if($this.val() === '' || $this.val() == $this.attr('placeholder')){
						  if(!$this.hasClass('password-input')){
							//add placeholder classname and set value to placeholder text
							$this.addClass(placeholderClass).val(placeholderText);
							}
						}
					})
					.blur();
				});
			}
			
			// coupled checkboxes
			if(coupledCheckboxes.length){
				coupledCheckboxes.each(function(){
					var checkboxes = $(this).find('input[type=checkbox]');
	
					// loop over each checkbox
					checkboxes.each(function(){
						// identify other checkboxes in this set
						var otherChecks = checkboxes.not($(this));
						
						// tie into iCheck 'change' event
						$(this).on('ifChanged', function(){
							// if this is now selected
							if($(this).attr('checked') == 'checked'){
								otherChecks.iCheck('uncheck');
							}
						});
					});
				});
				
			
			}
			
		});
		

	}
var initUiInputAppendDate = function(e) {
$('footer').append('<script src="/in/en/resources/js/jquey-ui-1.12.1.js"></script> ');    
$('.input-append.date').find('input.span2').datepicker({
	changeMonth:true,
	changeYear:true,
	showOn:"both",
	firstDay:0, // Start with Monday
	//dayNamesMin:"Sun Mon Tue Wed Thu Fri Sat".split(" "),
	dateFormat :"dd/mm/yy"
});
   
    var holidayDate = ["26-2-2017","29-2-2017","13-3-2017","23-3-2017"];  
$('.input-append.date').find('input.two-months').datepicker({
	changeMonth:false,
	changeYear:false,
	showOn:"both",
    numberOfMonths: 2,
    showButtonPanel: true,
    closeText: "X",
	firstDay:0,
	defaultDate: "$(this).val()",
	dateFormat :"d M yy",
    beforeShow: function(input, inst) {
       $('.ui-datepicker').addClass('customDatepicker');
       
    },
    onClose: function(input, inst) {
       $('.ui-datepicker').removeClass('customDatepicker');
    },
    beforeShowDay: function(date) {
        var m = date.getMonth(), 
            d = date.getDate(), 
            y = date.getFullYear();
            if(!$('.dayInfo').length){
            $('#ui-datepicker-div').append('<div class="dayInfo"><div class="today-date">Today"s Date</div><div class="holiday">Holiday</div></div>');
            }
            
        var formattedDate = d + '-' + (m+1) + '-' + y;
        
        if($.inArray(formattedDate,holidayDate) != -1) {
            return [true, 'holiday_date'];
        }
        return [true];
    }

});
    
    $('.hasDatepicker').datepicker()
	.on("input change", function (e) {
	$(this).siblings('.holder').css("display", "none");
});
}

/** AutoComplete **/
var initAutoComplete = function(e) {
$('footer').append('<link rel="stylesheet" href="/in/en/resources/css/autocomplete.css" type="text/css" /><script src="/in/en/resources/js/autocomplete.js"></script>');
$( function() {
var availableTags = [
  "Singapore",
  "Sinta, Portugal",
  "Sinaia, Romania",
  "Sinchon, Seoul",
  "Sintra, Sintra",
  "Singaraja, Bali"
];

	$( ".AutoCompleteTags").textext({plugins : 'tags autocomplete focus', html: {focus: "<span/>" }}).bind('getSuggestions', function(e, data)
	{
		var list = availableTags,
			textext = $(e.target).textext()[0],
			query = (data ? data.query : '') || '' 
			;
        $(".widget-wrap").removeClass("custom");   
		$(this).trigger(
			'setSuggestions',
			{ result : textext.itemManager().filter(list, query) }
		);
     $(".widget-wrap").addClass("custom");  
    
	});
    /* $(".text-tags").wrap( "<div class='text-tags-wrap'></div>" );*/    

})
};


/** AutoComplete **/
var initAutoCompleteSelected = function(e) {
$('footer').append('<link rel="stylesheet" href="/in/en/resources/css/autocomplete.css" type="text/css" /><script src="/in/en/resources/js/autocomplete.js"></script>');
$( function() {
var availableTags = [
  "Singapore",
  "Sinta, Portugal",
  "Sinaia, Romania",
  "Sinchon, Seoul",
  "Sintra, Sintra",
  "Singaraja, Bali"
];

$( "#holidayTagsSelected,.holidayTagsSelected").textext({
	tagsItems: [ 'Singapore', 'Malaysia' ],
	prompt : 'Add one...',
	plugins : 'tags autocomplete focus',
	html: {focus: "<span/>" }}).bind('getSuggestions', 
	function(e, data){
		var list = availableTags,
			textext = $(e.target).textext()[0],
			query = (data ? data.query : '') || '';	
		$(this).trigger(
			'setSuggestions',
			{ result : textext.itemManager().filter(list, query) }
		);
	})
})
};


var initUiCustomizeCalender2 = function(e) {
$('footer').append('<script src="/in/en/resources/js/jquey-ui-1.12.1.js"></script> '); 
$( function() {    
var disable_dates = ["1-3-2017","2-3-2017","3-3-2017","4-3-2017","5-3-2017","6-3-2017","7-3-2017"];
var available_dates = ["24-3-2017","25-3-2017","26-3-2017","27-3-2017"];
var arrival_dates = ["24-3-2017"];
var departure_dates = ["28-3-2017"];    
var destination_one_day = ["24-3-2017","25-3-2017","26-3-2017"];
var destination_two_day = ["27-3-2017","28-3-2017"];     
var holiday_dates = ["26-3-2017"];

$("#datepicker2").datepicker({
	dayNamesMin: "SU MO TU WE TH FR SA".split(" "),
    setDate : "currentDate",
    dateFormat: 'dd-mm-yy',
    showAnim: "fold",
    //minDate: 'disable_dates', 
    //maxDate: "+1M +10D",
    beforeShowDay: function(date) {
        var m = date.getMonth(), 
            d = date.getDate(), 
            y = date.getFullYear();
        var formattedDate = d + '-' + (m+1) + '-' + y; //date + '-' + (month+1) + '-' + year
        if($.inArray(formattedDate,holiday_dates) != -1) {
            return [true, 'extra-nyt destination-one-day pkg-nyt'];
        }
        if($.inArray(formattedDate,arrival_dates) != -1) {
            return [true, 'destination-one-day arrival-dates pkg-nyt'];
        }
        if($.inArray(formattedDate,departure_dates) != -1) {
            return [true, 'destination-two-day departure-dates pkg-nyt'];
        }
        if($.inArray(formattedDate,destination_one_day) != -1) {
            return [true, 'destination-one-day pkg-nyt'];
        } 
        if($.inArray(formattedDate,destination_two_day) != -1) {
            return [true, 'destination-two-day pkg-nyt'];
        }
         
        if($.inArray(formattedDate,available_dates) != -1) {
            return [true, 'pkg-nyt'];
        }
       
        if($.inArray(formattedDate,disable_dates) != -1) {
            return [true, 'disable-nyt'];
        }
        return [true];
    }
    
})//on('show', function(ev){$(this).addClass('onpage-cal-lg');});  
});
 $('.hasDatepicker').datepicker()
	.on("input change", function (e) {
	$(this).siblings('.holder').css("display", "none");
});    
}
var initUiCustomCalender = function(e) {
//$('footer').append('<script src="/in/en/resources/js/jquey-ui-1.12.1.js"></script> '); 
$( function() {    
var holidayDate = ["26-8-2017","29-8-2017"];

$("#custom_datepicker , .custom_datepicker").datepicker({
	dayNamesMin: "SU MO TU WE TH FR SA".split(" "),
    setDate : "currentDate",
    dateFormat: 'dd-mm-yy',
    showAnim: "fold",
    defaultDate: "$(this).val()",
   
    beforeShowDay: function(date) {
        var m = date.getMonth(), 
            d = date.getDate(), 
            y = date.getFullYear();
        if(!$('.date-legends').length){
            $('.custom_datepicker .ui-datepicker').append('<div class="date-legends"><div class="today-date">Today"s Date</div><div class="holiday">Holiday</div></div>');
            }
            var formattedDate = d + '-' + (m+1) + '-' + y; //date + '-' + (month+1) + '-' + year
        
        if($.inArray(formattedDate,holidayDate) != -1) {
            return [true, 'holiday_date'];
        }
        return [true];
    }
    
})//on('show', function(ev){$(this).addClass('onpage-cal-lg');});  
});
     $('.hasDatepicker').datepicker()
	.on("input change", function (e) {
	$(this).siblings('.holder').css("display", "none");
});
}



var initUiCustomizeCalenderMultiCity = function(e) {

var no_fares = ["1-2-2017","2-2-2017","3-2-2017","4-2-2017","5-2-2017","6-2-2017","7-2-2017","8-2-2017","9-2-2017","10-2-2017","1-3-2017","2-3-2017","3-3-2017","4-3-2017","5-3-2017","6-3-2017","7-3-2017","8-3-2017","9-3-2017"];  
var dayrates = ["12,000","7,200","12,000","7,200","7,200","9,000","9,000"];
var lowestRates= ["14-2-2017","15-2-2017","18-3-2017","19-3-2017"]
$('.input-append.date').find('input.datepickerMulticity').datepicker({
	changeMonth:false,
	changeYear:false,
	showOn:"both",
    numberOfMonths: 2,
    showButtonPanel: true,
    closeText: "X",
    //showButtonPanel: true,
	//dayNamesMin:"Sun Mon Tue Wed Thu Fri Sat".split(" "),
	firstDay:0,
	defaultDate: "$(this).val()",
	dateFormat :"d M yy",
    beforeShow: function(input, inst) {
       $('.ui-datepicker').addClass('customDatepicker');
       
    },
    onClose: function(input, inst) {
       $('.ui-datepicker').removeClass('customDatepicker');
    },
    beforeShowDay: function(date) {
        var m = date.getMonth(), 
            d = date.getDate(), 
            y = date.getFullYear(),
           selectable = true,
           classname = "lowest-fare",
           title = "â‚¹" + dayrates[date.getDay()];
           if(!$('.dayInfo').length){
            $('.customDatepicker').append('<div class="dayInfo"><div class="dayInfoCont"><span class="close-day-info">Lowest Fares</span></div></div>');
            } 
        var formattedDate = d + '-' + (m+1) + '-' + y;
        
        if($.inArray(formattedDate,no_fares) != -1) {
            return [true, 'no-fares'];
        }
        if($.inArray(formattedDate,lowestRates) != -1) {
            var m = date.getMonth(), 
            d = date.getDate(), 
            y = date.getFullYear(),
           selectable = true,
           classname = "lowest-fare  lowest-rates",
           title = "Ã¢â€šÂ¹" + dayrates[date.getDay()];
            
        var formattedDate = d + '-' + (m+1) + '-' + y;
            //return [true, 'lowest-fare'];
        }
        return [selectable, classname, title];


    }
});
  $('.hasDatepicker').datepicker()
	.on("input change", function (e) {
	$(this).siblings('.holder').css("display", "none");
});    
}

var initUiCustomTwoMonthsCalender = function(e) {
var holidayDate = ["26-2-2017","29-2-2017","13-3-2017","23-3-2017"];  
$('.input-append.date').find('input.CustomTwoMonthsCalendar').datepicker({
	changeMonth:false,
	changeYear:false,
	showOn:"both",
    numberOfMonths: 2,
    showButtonPanel: true,
    closeText: "X",
	firstDay:0,
	defaultDate: "$(this).val()",
	dateFormat :"d M yy",
    beforeShow: function(input, inst) {
       $('.ui-datepicker').addClass('customDatepicker');
       
    },
    onClose: function(input, inst) {
       $('.ui-datepicker').removeClass('customDatepicker');
    },
    beforeShowDay: function(date) {
        var m = date.getMonth(), 
            d = date.getDate(), 
            y = date.getFullYear();
            if(!$('.dayInfo').length){
            $('#ui-datepicker-div').append('<div class="dayInfo"><div class="today-date">Today"s Date</div><div class="holiday">Holiday</div></div>');
            }
            
        var formattedDate = d + '-' + (m+1) + '-' + y;
        
        if($.inArray(formattedDate,holidayDate) != -1) {
            return [true, 'holiday_date'];
        }
        return [true];
    }

});
     $('.hasDatepicker').datepicker()
	.on("input change", function (e) {
	$(this).siblings('.holder').css("display", "none");
});
}
