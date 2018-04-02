<!DOCTYPE html>
<!--[if lt IE 7]><html class="no-js ie6 oldie" lang="en"><![endif]-->
<!--[if IE 7]><html class="no-js ie7 oldie" lang="en"><![endif]-->
<!--[if IE 8]><html class="no-js ie8 oldie" lang="en"><![endif]-->
<!--[if gt IE 8]><!-->
<html class="no-js" lang="en">
<!--<![endif]-->

<head>
<meta charset="utf-8" />
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta name="viewport"
	content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
<title>Cox &amp; Kings</title>
<!-- header css and script starts -->
<meta name="apple-mobile-web-app-capable" content="yes">
<meta name="apple-mobile-web-app-status-bar-style" content="black">
<meta name="apple-mobile-web-app-title" content="Ezeego1 Application">
<link rel="apple-touch-icon"
	href="/in/en/resources/images/icons/icon-152x152.png">
<meta name="msapplication-TileImage"
	content="/in/en/resources/images/icons/icon-144x144.png">
<meta name="msapplication-TileColor" content="#2F3BA2">


<link
	href="http://ui.coxandkings.com/in/en/resources/css/bootstrap.min.css"
	rel="stylesheet">
<!-- Bootstrap core CSS -->
<link href="http://ui.coxandkings.com/in/en/resources/css/plugin.css"
	rel="stylesheet" type="text/css" media="all" />
<!-- Plugin CSS -->

<!-- Custom styles for this template -->
<link href="http://ui.coxandkings.com/in/en/resources/css/master.css"
	rel="stylesheet">
<!-- Media CSS -->

<!-- HTML5 shim and Respond.js for IE8 support of HTML5 elements and media queries -->
<!--[if lt IE 9]>
  <script type="text/javascript" src="/in/en/resources/js/ie/html5shiv.js"></script>
  <script type="text/javascript" src="/in/en/resources/js/ie/respond.min.js"></script>
<![endif]-->

<script type="text/javascript"
	src="http://ui.coxandkings.com/in/en/resources/js/jquery.min.js"></script>
<script type="text/javascript"
	src="http://ui.coxandkings.com/in/en/resources/js/modernizr-2.6.2.min.js"></script>
<script type="text/javascript"
	src="http://ui.coxandkings.com/in/en/resources/js/response.min.js"></script>


<!-- header css and script ends -->

<script type="text/javascript">
	$(document).ready(function() {
		$("#multiDiv").hide();
		$("#MulticityId").click(function() {
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

	function disableTextBox() {

		if (document.getElementById('OneWay').checked
				|| document.getElementById('MulticityId').checked
				|| document.getElementById('FlightHotel').checked) {

			document.getElementById('arrivalDate').disabled = true;
		} else {
			document.getElementById('arrivalDate').disabled = false;
		}
	}

	$(document).ready(function() {
		$("#originLocation").click(function() {
			$("#FromLabel").hide();
		});
		$("#originLocation").blur(function() {
			if ($("#originLocation").val().length == 0)
				$("#FromLabel").show();
		});
		$("#destinationLocation").click(function() {
			$("#ToLabel").hide();
		});
		$("#destinationLocation").blur(function() {
			if ($("#destinationLocation").val().length == 0)
				$("#ToLabel").show();
		});
		$("#departureDate").click(function() {
			$("#DepartId").hide();
		});
		$("#departureDate").blur(function() {
			if ($("#departureDate").val().length == 0)
				$("#DepartId").show();
		});
		$("#arrivalDate").click(function() {
			$("#arrivalId").hide();
		});
		$("#arrivalDate").blur(function() {
			if ($("#arrivalDate").val().length == 0)
				$("#arrivalId").show();
		});
		$("#adult").click(function() {
			$("#adultLabel").hide();
		});
		$("#adult").blur(function() {
			if ($("#adult").val().length == 0)
				$("#adultLabel").show();
		});
		$("#children").click(function() {
			$("#childrenLabel").hide();
		});
		$("#children").blur(function() {
			if ($("#children").val().length == 0)
				$("#childrenLabel").show();
		});
		$("#infant").click(function() {
			$("#infantLabel").hide();
		});
		$("#infant").blur(function() {
			if ($("#infant").val().length == 0)
				$("#infantLabel").show();
		});
		$("#cabinClass").click(function() {
			$("#cabinLabel").hide();
		});
		$("#cabinClass").blur(function() {
			if ($("#cabinClass").val().length == 0)
				$("#cabinLabel").show();
		});
		$("#originLocation1").click(function() {
			$("#FromLabel1").hide();
		});
		$("#originLocation1").blur(function() {
			if ($("#originLocation1").val().length == 0)
				$("#FromLabel1").show();
		});
		$("#destinationLocation1").click(function() {
			$("#ToLabel1").hide();
		});
		$("#destinationLocation1").blur(function() {
			if ($("#destinationLocation1").val().length == 0)
				$("#ToLabel1").show();
		});
		$("#departureDate1").click(function() {
			$("#DepartId1").hide();
		});
		$("#departureDate1").blur(function() {
			if ($("#departureDate1").val().length == 0)
				$("#DepartId1").show();
		});
		$("#originLocation2").click(function() {
			$("#FromLabel2").hide();
		});
		$("#originLocation2").blur(function() {
			if ($("#originLocation2").val().length == 0)
				$("#FromLabel2").show();
		});
		$("#destinationLocation2").click(function() {
			$("#ToLabel2").hide();
		});
		$("#destinationLocation2").blur(function() {
			if ($("#destinationLocation2").val().length == 0)
				$("#ToLabel2").show();
		});
		$("#departureDate2").click(function() {
			$("#DepartId2").hide();
		});
		$("#departureDate2").blur(function() {
			if ($("#departureDate2").val().length == 0)
				$("#DepartId2").show();
		});
	});
</script>

</head>

<body class="inner-pg flights" id="flights"
	data-responsejs='{"create":[{"prop":"width", "prefix":"min-device-width-", "lazy":false, "breakpoints":[0, 320, 481, 641, 767, 961, 1025, 1281] }]}'>
	<header>
		<div class="top-bar">
			<div class="container">
				<div class="top-bar-lt">
					<div class="logo">
						<a href="http://ui.coxandkings.com/in/en/index.shtml"><img
							src="/in/en/resources/images/common/cnk-logo.png" alt=""></a>
					</div>
					<span>Toll free:</span> <b><a href="tel:18002090400">1800
							209 0400</a></b> / <b><a href="tel:09867565599">098675 65599</a></b>
				</div>
				<div class="top-bar-rt">
					<ul>
						<li class="dropdown currency-dropdown"><a href="#"
							class="dropdown-toggle currency-main-menu" id="dropdownMenu1"
							data-toggle="dropdown" aria-haspopup="true" aria-expanded="true"><span>INR</span>
								<span class="fa fa-inr"></span><span class="caret"></span></a>

							<div class="dropdown-menu sub-menu currency">
								<ul class="">
									<li><a href="#" data-contryname="AUD"
										data-contrycurrency="$"><span class="curTick"></span>Australian
											Dollar<span>$</span> </a></li>
									<li><a href="#" data-contryname="BHD"
										data-contrycurrency="BHD"><span class="curTick"></span>Bahraini
											Dinar<span>BHD</span> </a></li>
									<li><a href="#" data-contryname="THB"
										data-contrycurrency="THB"><span class="curTick"></span>Baht<span>THB</span>
									</a></li>
									<li><a href="#" data-contryname="CAD"
										data-contrycurrency="$"><span class="curTick"></span>Canadian
											Dollar<span>$</span> </a></li>
									<li><a href="#" data-contryname="EUR"
										data-contrycurrency="fa fa-euro"><span class="curTick"></span>Euro<span
											class="fa fa-euro"></span> </a></li>
									<li><a href="#" data-contryname="HKD"
										data-contrycurrency="$"><span class="curTick"></span>Hong
											Kong Dollar<span>$</span> </a></li>
									<li><a href="#" data-contryname="INR"
										data-contrycurrency="fa fa-inr"><span class="curTick"></span>Indian
											Rupee<span class="fa fa-inr"></span> </a></li>
									<li><a href="#" data-contryname="IQD"
										data-contrycurrency="IQD"><span class="curTick"></span>Iraqi
											Dollar<span>IQD</span> </a></li>
									<li><a href="#" data-contryname="KWD"
										data-contrycurrency="KWD"><span class="curTick"></span>Kuwaiti
											Dinar<span>KWD</span> </a></li>
									<li><a href="#" data-contryname="MYR"
										data-contrycurrency="M$"><span class="curTick"></span>Malaysian
											Ringgit<span>M$</span> </a></li>
									<li><a href="#" data-contryname="NPR"
										data-contrycurrency="NPR"><span class="curTick"></span>Nepalese
											Rupee<span>NPR</span> </a></li>
									<li><a href="#" data-contryname="PKR"
										data-contrycurrency="Rs."><span class="curTick"></span>Pakistan
											Rupee<span>Rs.</span> </a></li>
									<li><a href="#" data-contryname="PHP"
										data-contrycurrency="PHP"><span class="curTick"></span>Philippine
											Peso<span>PHP</span> </a></li>
									<li><a href="#" data-contryname="GBP"
										data-contrycurrency="fa fa-gbp"><span class="curTick"></span>Pound
											Sterling<span class="fa fa-gbp"></span> </a></li>
									<li><a href="#" data-contryname="QAR"
										data-contrycurrency="QAR"><span
											class="curTick fa fa-check"></span>Qatari Rial<span>QAR</span>
									</a></li>
									<li><a href="#" data-contryname="ZAR"
										data-contrycurrency="$"><span class="curTick"></span>Rand<span>$</span>
									</a></li>
									<li><a href="#" data-contryname="OMR"
										data-contrycurrency="OMR"><span class="curTick"></span>Rial
											Omani<span>OMR</span> </a></li>
									<li><a href="#" data-contryname="IDR"
										data-contrycurrency="Rp"><span class="curTick"></span>Rupiah<span>Rp</span>
									</a></li>
									<li><a href="#" data-contryname="SAR"
										data-contrycurrency="SAR"><span class="curTick"></span>Saudi
											Riyal<span>SAR</span> </a></li>
									<li><a href="#" data-contryname="SGD"
										data-contrycurrency="fa fa-usd"><span class="curTick"></span>Singapore
											Dollar<span>$</span> </a></li>
									<li><a href="#" data-contryname="BDT"
										data-contrycurrency="BDT"><span class="curTick"></span>Taka<span>BDT</span>`
									</a></li>
									<li><a href="#" data-contryname="AED"
										data-contrycurrency="AED"><span class="curTick"></span>UAE
											Dirham<span>AED</span> </a></li>
									<li><a href="#" data-contryname="USD"
										data-contrycurrency="$"><span class="curTick"></span>US
											Dollar<span>$</span> </a></li>
									<li><a href="#" data-contryname="JPY"
										data-contrycurrency="fa fa-jpy"><span class="curTick"></span>Yen<span
											class="fa fa-jpy"></span> </a></li>
									<li><a href="#" data-contryname="CNY"
										data-contrycurrency="fa  fa-rmb"><span class="curTick"></span>Yuan
											Renminbi<span class="fa  fa-rmb"></span> </a></li>
								</ul>
							</div></li>
						<li class="country-dropdown"><a href="#"
							class="dropdown-toggle" id="dropdownMenu2" data-toggle="dropdown"
							aria-haspopup="true" aria-expanded="true"> <img
								src="/in/en/resources/images/common/icons/country-flag/ind.png"
								alt=""> <span class="caret"></span>
						</a>
							<div class="dropdown-menu theme-01">
								<ul>
									<li><a href="#"> <span class="flag-wrap"><img
												src="/in/en/resources/images/common/icons/country-flag/ind.png"
												alt=""></span> <span class="country-nm">India</span> <span
											class="country-lang">English | हिन्दी </span>
									</a></li>
									<li><a href="#"> <span class="flag-wrap"><img
												src="/in/en/resources/images/common/icons/country-flag/australia.png"
												alt=""></span> <span class="country-nm">Australia</span> <span
											class="country-lang">English</span>
									</a></li>
									<li><a href="#"> <span class="flag-wrap"><img
												src="/in/en/resources/images/common/icons/country-flag/dubai.png"
												alt=""></span> <span class="country-nm">Dubai</span> <span
											class="country-lang">English</span>
									</a></li>
									<li><a href="#"> <span class="flag-wrap"><img
												src="/in/en/resources/images/common/icons/country-flag/japan.png"
												alt=""></span> <span class="country-nm">Japan</span> <span
											class="country-lang">English | 日本語</span>
									</a></li>
									<li><a href="#"> <span class="flag-wrap"><img
												src="/in/en/resources/images/common/icons/country-flag/uk.png"
												alt=""></span> <span class="country-nm">UK</span> <span
											class="country-lang">English</span>
									</a></li>
									<li><a href="#"> <span class="flag-wrap"><img
												src="/in/en/resources/images/common/icons/country-flag/usa.png"
												alt=""></span> <span class="country-nm">USA</span> <span
											class="country-lang">English</span>
									</a></li>
								</ul>
							</div></li>
						<li class="nav-custom-dl"><a
							href="/in/en/general/download-app.shtml"><i
								class="fa-custom-dl-icon"></i></a></li>
						<li class="nav-gift-cards"><a href="/in/en/gift-cards.shtml"><i
								class="fa-gift"></i><span>Gift Cards</span></a></li>
						<li class="nav-recently-viewed"><a href="#"
							class="dropdown-toggle" id="dropdownMenu3" data-toggle="dropdown"
							aria-haspopup="true" aria-expanded="true"><i class="fa-eye"></i><span>Recently
									Viewed</span><span class="badge">2</span></a>
							<div class="dropdown-menu theme-02">
								<ul>
									<li><a href="#" class="cont-wrap"> <span
											class="icon-wrapper"><i class="fa-custom-hotel"></i></span> <span
											class="txt-wrapper"> <span class="txt-ttl">Alila
													Diwa Goa</span> <span class="sub-txt">4 nights, Thu, 07 Sep
													- Sun, 10 Sep 2017</span>
										</span> <span class="price-wrapper"><i class="fa-rupee"></i>
												3,050</span>

									</a> <a class="cls-iconwrap" href="#"><i
											class="fa-times-circle"></i></a></li>
									<li><a href="#" class="cont-wrap"> <span
											class="icon-wrapper"> <i class="fa-custom-car-purple"></i>
										</span> <span class="txt-wrapper"> <span class="txt-ttl">Ford
													Figo - Oneway</span> <span class="txt-ttl">Kala Ghoda -
													Andheri</span> <span class="sub-txt">SUN, 26 Feb 2017,
													12:00 PM, 1Hr</span>
										</span> <span class="price-wrapper"><i class="fa-rupee"></i>
												800</span>

									</a> <a class="cls-iconwrap" href="#"><i
											class="fa-times-circle"></i></a></li>
								</ul>
							</div></li>
						<li class="nav-my-wishlist"><a href="#"
							class="dropdown-toggle" id="dropdownMenu4" data-toggle="dropdown"
							aria-haspopup="true" aria-expanded="true"><i
								class="fa-custom-heart-blue"></i><span>My wishlist</span><span
								class="badge">2</span></a>
							<div class="dropdown-menu theme-02 type2">
								<ul>
									<li><a href="#" class="cont-wrap"> <span
											class="icon-wrapper"><i
												class="fa-custom-baggage-purple"></i></span> <span
											class="txt-wrapper"> <span class="txt-ttl">Singapore
													Magic</span> <span class="sub-txt">4 Days / 3 Nights</span>
										</span> <span class="price-wrapper"><i class="fa-rupee"></i>
												38,500</span>

									</a> <a class="cls-iconwrap" href="#"><i
											class="fa-times-circle"></i></a></li>
									<li><a href="#" class="cont-wrap"> <span
											class="icon-wrapper"> <i class="fa-custom-car-purple"></i>
										</span> <span class="txt-wrapper"> <span class="txt-ttl">Ford
													Figo - Oneway</span> <span class="txt-ttl">Kala Ghoda -
													Andheri</span> <span class="sub-txt">SUN, 26 Feb 2017,
													12:00 PM, 1Hr</span>
										</span> <span class="price-wrapper"><i class="fa-rupee"></i>
												800</span>

									</a> <a class="cls-iconwrap" href="#"><i
											class="fa-times-circle"></i></a></li>
									<li class="btn-sec"><a href="#"
										class="btn btn-primary btn-white">VIEW ALL</a></li>
								</ul>
							</div></li>
						<li class="nav-my-cart"><a href="#" class="dropdown-toggle"
							id="dropdownMenu5" data-toggle="dropdown" aria-haspopup="true"
							aria-expanded="true"><i class="fa-custom-cart-blue"></i><span>My
									Cart</span><span class="badge">3</span></a>
							<div class="dropdown-menu theme-02 cart">
								<ul>
									<li><a href="#" class="cont-wrap"> <span
											class="icon-wrapper"><i class="fa-custom-hotel"></i></span> <span
											class="txt-wrapper"> <span class="txt-ttl">Park
													Regency Ocean</span> <span class="sub-txt">4 nights, Thu,
													07 Sep - Sun, 10 Sep 2017</span>
										</span> <span class="price-wrapper"><i class="fa-rupee"></i>
												86,520</span>

									</a> <a class="cls-iconwrap" href="#"><i
											class="fa-times-circle"></i></a></li>
									<li><a href="#" class="cont-wrap"> <span
											class="icon-wrapper"><i
												class="fa-custom-baggage-purple"></i></span> <span
											class="txt-wrapper"> <span class="txt-ttl">Singapore
													Magic</span> <span class="sub-txt">4 Days / 3 Nights</span>
										</span> <span class="price-wrapper"><i class="fa-rupee"></i>
												38,500</span>

									</a> <a class="cls-iconwrap" href="#"><i
											class="fa-times-circle"></i></a></li>
									<li><a href="#" class="cont-wrap"> <span
											class="icon-wrapper"> <i class="fa-custom-car-purple"></i>
										</span> <span class="txt-wrapper"> <span class="txt-ttl">Ford
													Figo - Oneway</span> <span class="txt-ttl">Kala Ghoda -
													Andheri</span> <span class="sub-txt">SUN, 26 Feb 2017,
													12:00 PM, 1Hr</span>
										</span> <span class="price-wrapper"><i class="fa-rupee"></i>
												800</span>

									</a> <a class="cls-iconwrap" href="#"><i
											class="fa-times-circle"></i></a></li>
									<li class="summary-sec">
										<div class="tot-prc">
											<span class="txt">TOTAL PRICE</span> <span class="amt"><i
												class="fa-rupee"></i> 1,25,820</span>
										</div> <a href="#" class="btn btn-primary btn-checkout">CHECKOUT</a>
									</li>
								</ul>
							</div></li>
						<li class="login after-lgn"><a href="#"
							class="dropdown-toggle" data-toggle="dropdown"
							aria-haspopup="true" aria-expanded="true"> <span> <span
									class="author-img"> <i class="fa-custom-login"></i>

								</span> <span class="login-txt">Login</span> <span class="caret"></span>
							</span>
						</a>
							<div class="dropdown-menu theme-03 login-sec">
								<ul>
									<li><a href="/in/en/login/login.shtml"
										class="btn btn-primary login-btn">LOGIN</a>
										<p class="no-account">Don’t have an account yet?</p> <a
										href="/in/en/signup/signup.shtml"
										class="btn btn-create-account">Create an account</a></li>

									<li><a href="/in/en/general/manage-booking.shtml"
										class="cont-wrap"><i class="fa-angle-right"></i>Manage
											Booking</a></li>
									<li><a class="cont-wrap popup-inline"
										href="#flight-search-result-page-fare-alert"><i
											class="fa-angle-right"></i>Fare Alerts</a></li>
									<li><a class="cont-wrap popup-inline"
										href="#login-to-access-flight-status"><i
											class="fa-angle-right"></i>Flight Status</a></li>
								</ul>
							</div></li>

						<li class="login before-lgn"><a href="#"
							class="dropdown-toggle" id="dropdownMenu3a"
							data-toggle="dropdown" aria-haspopup="true" aria-expanded="true">
								<span class="user1"> <span class="author-img"> <img
										src="/in/en/resources/images/common/author-img.jpg" alt=""
										class="authorImg">
								</span> <span class="author-nme">Kaushal</span> <span class="caret"></span>
							</span> <span class="user2" style="display: none;"> <span
									class="author-img"> <img
										src="/in/en/resources/images/common/customer-3.png" alt=""
										class="authorImg">
								</span> <span class="author-nme">Reema</span> <span class="caret"></span>
							</span>
						</a>
							<div class="dropdown-menu theme-03 logout-sec">
								<ul>
									<li><a href="/in/en/my-account/my-profile.shtml"
										class="cont-wrap"><i class="fa-angle-right"></i>My Profile</a></li>
									<li><a href="/in/en/my-account/my-bookings.shtml"
										class="cont-wrap"><i class="fa-angle-right"></i>My
											Bookings</a></li>
									<li><a href="/in/en/general/manage-booking.shtml"
										class="cont-wrap"><i class="fa-angle-right"></i>Manage
											Booking</a></li>
									<li><a href="/in/en/my-account/my-e-wallet.shtml"
										class="cont-wrap"><i class="fa-angle-right"></i>e-Wallet</a></li>
									<li><a class="cont-wrap popup-inline"
										href="#flight-search-result-page-fare-alert"><i
											class="fa-angle-right"></i>Fare Alerts</a></li>
									<li><a class="cont-wrap popup-inline"
										href="#login-to-access-flight-status"><i
											class="fa-angle-right"></i>Flight Status</a></li>
									<li><a href="#" class="cont-wrap btn-logout"><i
											class="fa-custom-angle-right"></i>Logout</a></li>
								</ul>
							</div></li>
					</ul>
				</div>
			</div>
		</div>
		<div class="main-header">
			<div class="container">
				<div class="main-header-cont">
					<div class="logo">
						<a href="/in/en/index.shtml"> <img
							src="/in/en/resources/images/common/cnk-logo.svg" alt=""
							class="svg-img"> <img
							src="/in/en/resources/images/common/cnk-logo.png" alt=""
							style="display: none">
						</a>
					</div>
					<a class="toggleMenu pull-right" href="javascript:void(0)"
						style="display: none;"><i class="fa-reorder"></i></a>
					<div class="desktopnav">
						<nav class="navbar">
							<ul class="nav">
								<li class="holidays"><a href="/in/en/index.shtml"
									class="nav-holidays">Holidays</a></li>
								<li class="destinations"><a
									href="/in/en/index-destination.shtml" class="nav-destinations">Destinations</a></li>
								<li class="collections"><a href="/in/en/collections.shtml"
									class="nav-collections">Collections</a></li>
								<li class="deals"><a href="/in/en/index-deals.shtml"
									class="nav-deals">Deals </a></li>
								<li class="blog"><a href="#" class="nav-blog">Blog</a></li>
							</ul>
						</nav>
						<div class="header-top">
							<ul>
								<li class="nav-activites"><a
									href="/in/en/index-activities.shtml">Activities</a></li>
								<li class="nav-flights"><a
									href="/in/en/index-flights.shtml">Flights</a></li>
								<li class="nav-hotels"><a href="/in/en/index-hotel.shtml">Hotels</a></li>
								<li class="nav-flights-hotels"><a
									href="/in/en/index-flights-hotels.shtml">Flights + Hotels</a></li>
								<li class="nav-cars"><a href="/in/en/index-cars.shtml">Car</a></li>
								<li class="nav-cruise"><a href="/in/en/index-cruise.shtml">Cruise</a></li>
								<li class="nav-more dropdown"><a href="#"
									class="dropdown-toggle" data-toggle="dropdown"
									aria-haspopup="true" aria-expanded="true">More <i
										class="caret"></i></a>
									<ul class="dropdown-menu">
										<li class="nav-rail"><a href="/in/en/index-rail.shtml">IRCTC
												Rail</a></li>
										<li class="nav-bus"><a href="/in/en/index-bus.shtml">Bus</a></li>
										<li class="nav-insurance"><a
											href="/in/en/index-insurance.shtml">Insurance</a></li>
										<li class="nav-visa"><a href="/in/en/index-visa.shtml">Visa</a></li>
										<li class="nav-railworldwide"><a
											href="/in/en/index-rail-worldwide.shtml">rail - worldwide</a></li>
									</ul></li>
							</ul>
						</div>
					</div>
				</div>
			</div>
		</div>
		<div class="mobile-nav container"></div>
		<div class="clear"></div>
	</header>
	<div class="floating-btns">
		<ul>
			<li><a href="#enq-popup" id="envelopeFloat"><i
					class="fa-envelope-o"></i></a>
				<div class="Float-box enquiry-form" id="enq-popup">
					<div class="ttl">
						Send Enquiry <a href="#" class="close-btn"><i
							class="fa-custom-close-icon"></i></a>
					</div>
					<div class="form form-sec">
						<div class="form-group">
							<div class="row row-sm">
								<div class="col-xs-12">
									<div class="row row-sm">
										<div class="col-md-2 col-sm-2 col-xs-3">
											<select class="">
												<option value="Mr" selected="selected">Mr.</option>
												<option value="Mrs">Mrs.</option>
												<option value="Ms">Ms.</option>
												<option value="Miss">Miss.</option>
											</select>
										</div>
										<div class="col-md-4 col-sm-4 col-xs-4">
											<input type="text" placeholder="First Name" />
										</div>
										<div class="col-md-6 col-sm-6 col-xs-5">
											<input type="text" placeholder="Last Name" />
										</div>
									</div>
								</div>
							</div>
						</div>


						<div class="form-group">
							<div class="row row-sm">
								<div class="col-xs-12">
									<div class="row row-sm">
										<div class="col-md-2 col-sm-2 col-xs-2">
											<input type="text" placeholder="+91" /> <em class="sub-tl">(Must
												be reachable)</em>
										</div>
										<div class="col-md-4 col-sm-4 col-xs-4">
											<input type="text" placeholder="Mobile No." />
										</div>
										<div class="col-md-6 col-sm-6 col-xs-6">
											<input type="text" placeholder="Email" /> <em class="sub-tl">(Must
												be contactable)</em>

										</div>
									</div>
								</div>
							</div>
						</div>

						<div class="form-group">
							<select class="">
								<option value="opt1" selected="selected">Select Product</option>
								<option>option 1</option>
								<option>option 2</option>
								<option>option 3</option>
							</select>
						</div>

						<div class="form-group">
							<textarea placeholder="Your Query (Max 200 characters)"></textarea>
						</div>

						<div class="form-group">
							<div class="row row-sm">
								<div class="col-xs-12">
									<div class="row row-sm">
										<div class="col-md-6 col-sm-6 col-xs-6">
											<span class="captcha-code-sec"> <img
												src="/in/en/resources/images/common/img-captcha-code.jpg"
												alt="">
											</span>
										</div>
										<div class="col-md-6 col-sm-6 col-xs-6">
											<input type="text" placeholder="Enter Code Here" />


										</div>
									</div>
								</div>
							</div>
						</div>
						<a href="#enq-popup-sucess" class="btn btn-primary" id="msgSec">Submit
							Query</a>
					</div>
				</div>
				<div class="Float-box enquiry-form-sucess" id="enq-popup-sucess">
					<div class="ttl">
						Send Enquiry <a href="#" class="close-btn"><i
							class="fa-custom-close-icon"></i></a>
					</div>
					<div class="msg-sec">
						<p class="success-msg">Query submitted!</p>
						<p>Thank you for enquiring with Cox &amp; Kings.</p>
						<p>
							Your enquiry reference number is <a href="#">#080916/291053</a>
						</p>
						<p>Our travel expert will get in touch with you soon.</p>
						<p>
							Incase you wish to get in touch with us, call us on our toll free
							number at <b>1800 209 0800</b>
						</p>
					</div>
				</div></li>
			<li><a href="#tollfree-popup" id="tollfreeFloat"><i
					class="fa-phone"></i></a>
				<div class="Float-box tollfree-call" id="tollfree-popup">
					<div class="ttl">
						Call toll free <a href="#" class="close-btn"><i
							class="fa-custom-close-icon"></i></a>
					</div>
					<span class="toll-no">1800 120 0660 / 098675 65599</span>
				</div></li>
			<li><a href="#chat-popup" id="chatFloat"><i
					class="fa-comments-o"></i></a>
				<div class="Float-box chat-form" id="chat-popup">
					<div class="ttl">
						Chat with travel expert! <a href="#" class="close-btn"><i
							class="fa-custom-close-icon"></i></a>
					</div>
					<div class="form form-sec">
						<div class="form-group">
							<input type="text" placeholder="Full Name" />
						</div>
						<div class="form-group">
							<select class="">
								<option value="opt1" selected="selected">Select Product</option>
								<option>option 1</option>
								<option>option 2</option>
								<option>option 3</option>
							</select>
						</div>
						<div class="form-group">
							<div class="row row-sm">
								<div class="col-xs-12">
									<div class="row row-sm">
										<div class="col-md-3 col-sm-3 col-xs-4 ">
											<input type="text" class="form-control" value="+91">
										</div>
										<div class="col-md-9 col-sm-9 col-xs-8">
											<input type="text" class="form-control"
												placeholder="Mobile No. (Must be reachable)">
										</div>
									</div>
								</div>
							</div>
						</div>
						<div class="form-group">
							<input type="text" placeholder="Email (Must be contactable)" />
						</div>
						<a href="#chat-sec-popup" class="btn btn-primary" id="chatSec">Initiate
							Chat</a>
					</div>
				</div>
				<div class="Float-box chat-sec" id="chat-sec-popup">
					<div class="ttl">
						Chat with travel expert! <a href="#" class="close-btn"><i
							class="fa-custom-close-icon"></i></a>
					</div>
					<div class="chat-window">
						<p class="text-center">Questions? We'd love to chat!</p>
						<div class="chatbox">
							<div class="input-wrap">
								<input type="text"
									placeholder="Type here and hit <enter> to chat"> <a
									href="#"><i class="fa-custom-send"></i></a>
							</div>
						</div>
					</div>
				</div></li>
		</ul>
		<span class="menuTrigger btn-filter"></span>
	</div>
	<div
		class="flight-status popup-box book-now-popup mfp-hide zoom-anim-dialog popup-sm popup-sec"
		id="pick-your-flight-overlay-by-flights">
		<div class="popup-in">
			<form>
				<h4>Pick your Flight</h4>
				<div class="para">
					<p>9W 411 Flight Status</p>
					<p>This Flight has multiple segments. Please select a segment
						from the list.</p>
					<p class="dtls">
						<a href="/in/en/flights/flights-status-result-by-flight.shtml">9W
							411 from (AMD) Ahmedabad to (BOM) Mumbai</a>
					</p>
					<p>
						<a href="/in/en/flights/flights-status-result-by-flight.shtml">9W
							411 from (BOM) Mumbai to (BLR) Bengaluru</a>
					</p>
				</div>
			</form>
		</div>
	</div>
	<div
		class="popup-box zoom-anim-dialog popup-sm mfp-hide login-flight-status-popup"
		id="login-to-access-flight-status">
		<div class="popup-in form-theme2">
			<form>
				<div class="signin-box">
					<div class="fltpop-sts-title">Flight Status</div>
					<label class="popfmtitle">Sign in via Cox &amp; Kings login
						ID</label>
					<div class="row">
						<div class="col-sm-6">
							<div class="signin-form">
								<span class="vert-seprter"><span> or </span> </span>
								<div class="login-sec">

									<div class="form-group">
										<label for="emailID1" class="frm-txt-sub md">Your
											email address</label> <input type="text" id="emailID1" placeholder="">
									</div>
									<div class="form-group">
										<label class="checkbox-container"><input
											type="checkbox" id="chk-password1"> I have a Cox &
											Kings password</label>
									</div>
									<div class="form-group pw-froup" style="display: none;">
										<label class="frm-txt-sub md">Your Password</label> <input
											type="password" placeholder="" class="txtbox"> <a
											class="txt-lnk darkblue lnk-frgt-pw" href="#">Forgot your
											password?</a>
									</div>
									<button type="button" class="btn btn-blue btn-login"
										onclick="javascript:location.href='/in/en/flights/flights-status-result-by-flight.shtml'"
										style="display: none;">Log In</button>
									<button type="button" class="btn btn-blue btn-login-cont"
										onclick="javascript:location.href='/in/en/flights/flights-status-result-by-flight.shtml'">
										Continue</button>
								</div>
								<div class="forgot-sec" style="display: none">
									<div class="form-group">
										<label class="label-head">New password for your
											account</label>
										<div class="forgot-note">
											<span>We shall verify your Username & Date of Birth
												and send you the new password via email.</span>
										</div>
									</div>
									<div class="form-group">
										<label for="userName1" class="frm-txt-sub">Username</label> <input
											type="text" id="userName1">
									</div>
									<div class="form-group">
										<label for="DOB1" class="frm-txt-sub">Date of Birth</label>
										<div class="input-append date" data-date-format="dd-mm-yyyy">
											<input type="text" placeholder="DD/MM/YY" id="DOB1"
												class="span2" /> <span class="add-on"></span>
										</div>
									</div>
									<button type="button" class="btn btn-blue btn-frgt-submit">
										Submit</button>
								</div>
								<div class="forgot-success-sec" style="display: none">
									<label class="label-head">New password for your account</label>
									<div class="forgot-note">
										<span class="msg-success"> <i
											class="fa fa-check-circle"></i> We have sent a link on <a
											href="#" class="txt-lnk darkblue lnk-reset-pw">kaushal.ranpura@hotmail.com</a>.<br>
											Please click on the link to reset your password.
										</span>
									</div>
								</div>
								<div class="reset-pw-sec" style="display: none">
									<div class="form-group">
										<label class="label-head">Reset your password</label>
										<div class="forgot-note">
											<span>A Strong Password is a combination of letters,
												punctuation mark and is 6 characters long.</span>
										</div>
									</div>
									<div class="form-group">
										<label for="Newpassword1" class="frm-txt-sub">Enter
											New Password </label> <input type="text" id="Newpassword1">
									</div>
									<div class="form-group">
										<label for="Confirmpassword1" class="frm-txt-sub">Confirm
											Password </label> <input type="text" id="Confirmpassword1">
									</div>
									<button type="button" class="btn btn-blue btn-new-login">
										Submit</button>
								</div>
								<div class="new-login-sec" style="display: none">
									<div class="form-group">
										<label class="label-head">Password changed
											successfully!</label>
										<div class="forgot-note">
											<span>You have reset your password. Please login with
												your new password.</span>
										</div>
									</div>
									<div class="form-group">
										<label for="newEmail1" class="frm-txt-sub">Email
											Address </label> <input type="text" id="newEmail1"
											value="kaushal.ranpura@hotmail.com">
									</div>
									<div class="form-group">
										<label for="newLoginPw1" class="frm-txt-sub">Enter
											Password </label> <input type="text" id="newLoginPw1">
									</div>
									<button type="button" class="btn btn-blue btn-frgt-submit"
										onclick="javascript:location.href='checkout-travelers.shtml'">
										Submit</button>
								</div>
							</div>


						</div>
						<div class="col-sm-6">
							<div class="sigin-social">
								<a href="" class="sbtn btn-fb"> <i
									class="fa-custom-facebook"></i> Sign in with Facebook
								</a> <a href="" class="sbtn btn-g"> <i class="fa-custom-google"></i>
									Sign in with Google
								</a>
							</div>
							<div class="signup-lnk">
								<span class="qstn">Don’t have an account yet?</span> <a
									href="/in/en/signup/signup.shtml" class="txt-lnk darkblue">Create
									an account</a>
							</div>
						</div>
						<div class="clearfix"></div>
					</div>
				</div>
			</form>
		</div>
	</div>
	<div
		class="popup-box zoom-anim-dialog tbl-shadow-wrap popup-sec mfp-hide flight-fare-alert"
		id="flight-search-result-page-fare-alert">
		<div class="popup-in form-theme05">
			<form>
				<div class="my-account">
					<div class="fltpop-sts-title">Set Flight Alert</div>
					<div class="flt-status-frmpop-boxs">
						<div class="form-group-sec">
							<div class="row row-md">
								<div class="col-xs-6 col-sm-6 mobile-12">
									<input type="text" class="form-control"
										placeholder="From City / Airport"
										data-placeholder="From City / Airport">
								</div>
								<div class="col-xs-6 col-sm-6 mobile-12">
									<input type="text" class="form-control"
										placeholder="To City / Airport"
										data-placeholder="To City / Airport">
								</div>
								<div class="col-xs-12 col-sm-12">
									<h4>When do you want to Travel?</h4>
									<span> <label>Fixed Dates <input type="radio"
											name="fare_alert_radio" id="fixdates" />
									</label>
									</span> <span> <label>Flexible Dates <input
											type="radio" name="fare_alert_radio" id="flexibledates" />
									</label>
									</span>
								</div>
								<div class="col-xs-6 col-sm-6 mobile-12" id="fixdates_wrap">
									<div class="input-append date" data-date-format="dd-mm-yyyy">
										<input class="span2" size="16" type="text"
											placeholder="Select Dates" name="to" /> <span class="add-on"></span>
									</div>
								</div>
								<div class="col-xs-6 col-sm-6 mobile-12" style="display: none;"
									id="flexibledates_wrap">
									<div class="extra-service-in">
										<select>
											<option selected="selected">Depart</option>
											<option>Anytime</option>
											<option>Upcoming weekends</option>
											<option>Aug 2017</option>
											<option>Sep 2017</option>
											<option>Oct 2017</option>
										</select>
									</div>
								</div>
								<div class="col-sm-6 col-xs-6 mobile-12">
									<div class="extra-service-in">
										<div class="multiselect">
											<div class="selectBox" onclick="showCheckboxes1()">
												<select class="selectpicker" multiple="multiple">
													<option selected="selected">Send Every</option>
													<option>Monday</option>
													<option>Tuesday</option>
													<option>Wednesday</option>
													<option>Thursday</option>
													<option>Friday</option>
													<option>Saturday</option>
													<option>Sunday</option>
												</select>
											</div>
										</div>
									</div>
								</div>
								<div class="col-xs-6 col-sm-6 mobile-12">
									<input type="text" class="form-control"
										placeholder="Send email alerts on"
										data-placeholder="Send email alerts on"> <span
										class="lbl-flds">(Must be contactable) </span>
								</div>
								<div class="col-xs-6 col-sm-6 mobile-12">
									<div class="form-group-sec">
										<div class="row row-md">
											<div class="col-sm-3 col-xs-3">
												<input type="text" class="form-control" value="+ 91">
											</div>
											<div class="col-sm-9 col-xs-9">
												<input type="text" class="form-control"
													placeholder="Send mobile notification"
													data-placeholder="Send mobile notification"> <span
													class="lbl-flds">(Must be reachable)</span>
											</div>
										</div>
									</div>
								</div>
							</div>
						</div>
						<div class="row">
							<div class="col-sm-12 text-center">
								<div class="button">
									<a href="#flight-status-Success-Confirmation"
										class="popup-inline popup-inline btn btn-blue">Save Alert</a>
								</div>
							</div>
						</div>
					</div>
				</div>
			</form>
		</div>
	</div>

	<div
		class="popup-box zoom-anim-dialog tbl-shadow-wrap mfp-hide popup-box popup-sec flight-fare-alert flight-alert-sucs-confirmation"
		id="flight-status-Success-Confirmation">
		<div class="popup-in">
			<form>
				<div class="flt-tle">
					<i class="fa-custom-check-circle-green-white"></i> Success !
				</div>
				<div class="para">
					<p>Your fare alert has been set! You will get an email soon.</p>
					<p>You can unsubscribe fare alert through your email.</p>
					<p class="dtls">
						<a href="#">Set more fare alerts</a>
					</p>
				</div>
				<div class="or-bor">
					<h3>OR</h3>
				</div>
				<div class="row">
					<div class="col-sm-12 text-center">
						<div class="button">
							<a href="#" class="btn btn-blue close-popup">Close</a>
						</div>
					</div>
				</div>
			</form>
		</div>
	</div>
	<div class="wrapper">
		<!-- Inner Banner ::Starts -->
		<!--Banner : Starts-->
		<div class="banner">
			<div class="banner-img-fit">
				<figure>
					<img
						src="/in/en/resources/images/flights-hotels/inner-banner-img.jpg"
						alt="" class="desktop-purpose">
					<img
						src="/in/en/resources/images/flights-hotels/inner-banner-img-767x350.jpg"
						alt="" class="mobile-purpose">
				</figure>
			</div>
			<!--Banner Search Sec - Starts-->
			<form method="post" action="getDetails">
				<div class="search-sec flights-wrp">
					<div class="container">
						<div class="banner-cont-wrap">


							<div class="form-sec">
								<h2 class="ttl">Flights</h2>
								<div class="widget-wrap">
									<div class="chekbox-radio-group black-theme chk-bx-cont">
										<label class="radio-inline selected"><input
											type="radio" value="OneWay" class="fl-one-way"
											name="tripType" id="OneWay" onclick="disableTextBox()"
											checked>One way</label> <label class="radio-inline"><input
											type="radio" value="Return" class="fl-return"
											name="tripType" id="Return" onclick="disableTextBox()">Return</label>
										<label class="radio-inline"><input type="radio"
											value="Multicity" class="fl-multicity" name="tripType"
											id="MulticityId" onclick="disableTextBox()">Multi
											city</label> <label class="radio-inline"><input type="radio"
											value="fl-flights-hotels" class="fl-flights-hotels"
											name="tripType" id="FlightHotel" onclick="disableTextBox()">Flights
											+ Hotels</label>
									</div>
									<ul class="widget-form-elements srch-oneway-return">
										<li class="custom-field frm-field" style="width: 14%">
											<div class="holder-wrap">
												<div id="FromLabel" class="holder">
													<label>From</label> <i>City, Airport</i>
												</div>
											</div> <input name="originLocation" id="originLocation" value=""
											class="AutoCompleteTags">
										</li>
										<li class="custom-field" style="width: 14%">
											<div id="ToLabel" class="holder">
												<label>To</label> <i>City, Airport</i>
											</div> <input name="destinationLocation" id="destinationLocation"
											value="" class="AutoCompleteTags">
										</li>
										<li class="custom-field" style="width: 14%">
											<div id="DepartId" class="holder">
												<label>Departure</label> <i>Date</i>
											</div> <input name="departureDate" id="departureDate" value=""
											class="AutoCompleteTags" />
										</li>
										<li class="custom-field" style="width: 14%">
											<div id="arrivalId" class="holder">
												<label>Return</label> <i>Date</i>
											</div> <input name="arrivalDate" id="arrivalDate" value=""
											class="AutoCompleteTags" disabled="disabled" />
										</li>
										<li class="custom-field" style="width: 14%">
											<div id="adultLabel" class="holder">
												<label>Adult</label> <i>Passenger</i>
											</div> <input name="adult" id="adult" value=""
											class="AutoCompleteTags" />
										</li>
										 <li class="custom-field" style="width: 14%">
											<div id="childrenLabel" class="holder">
												<label>Children</label> <i>Passenger</i>
											</div> <input name="children" id="children" value=""
											class="AutoCompleteTags" />
										</li> 
										<li class="custom-field" style="width: 14%">
											<div id="infantLabel" class="holder">
												<label>Infant</label> <i>Passenger</i>
											</div> <input name="infant" id="infant" value=""
											class="AutoCompleteTags" />
										</li>
										<li class="custom-field" style="width: 14%">
											<div id="cabinLabel" class="holder">
												<label>Cabin</label> <i>class</i>
											</div> <input name="cabinClass" id="cabinClass" value=""
											class="AutoCompleteTags" />
										</li>
										<li class="search-btn"><input type="submit" name="search"
											value="Search" class="btn btn-primary btn-return"></li>
									</ul>


									<div id="multiDiv">
										<ul class="widget-form-elements srch-oneway-return">
											<li class="custom-field frm-field tags-where"
												style="width: 14%">
												<div class="holder-wrap">
													<div id="FromLabel1" class="holder">
														<label>From</label> <i>City, Airport</i>
													</div>
												</div> <input name="originLocation1" id="originLocation1" value=""
												class="AutoCompleteTags"> <!--<a href="#" class="btn edit-btn"><i class="fa-custom-landmark"></i></a>-->
											</li>
											<li class="custom-field" style="width: 14%">
												<div id="ToLabel1" class="holder">
													<label>To</label> <i>City, Airport</i>
												</div> <input name="destinationLocation1"
												id="destinationLocation1" value="" class="AutoCompleteTags">

											</li>
											<li class="custom-field" style="width: 14%">
												<div id="DepartId1" class="holder">
													<label>Departure</label> <i>Date</i>
												</div> <input name="departureDate1" id="departureDate1" value=""
												class="AutoCompleteTags" />
											</li>
										</ul>

										<ul class="widget-form-elements srch-oneway-return">
											<li class="custom-field frm-field tags-where"
												style="width: 14%">
												<div class="holder-wrap">
													<div id="FromLabel2" class="holder">
														<label>From</label> <i>City, Airport</i>
													</div>
												</div> <input name="originLocation2" id="originLocation2" value=""
												class="AutoCompleteTags"> <!--<a href="#" class="btn edit-btn"><i class="fa-custom-landmark"></i></a>-->
											</li>
											<li class="custom-field" style="width: 14%">
												<div id="ToLabel2" class="holder">
													<label>To</label> <i>City, Airport</i>
												</div> <input name="destinationLocation2"
												id="destinationLocation2" value="" class="AutoCompleteTags">

											</li>
											<li class="custom-field" style="width: 14%">
												<div id="DepartId2" class="holder">
													<label>Departure</label> <i>Date</i>
												</div> <input name="departureDate2" id="departureDate2" value=""
												class="AutoCompleteTags" />
											</li>
										</ul>
									</div>

								</div>
							</div>
						</div>
					</div>
				</div>
				<div>
				<input type="hidden" id="transactionID" name="transactionID" value="12345"> 
					<input type="hidden" id="sessionID"
					name="sessionID" value="SID101"> <input type="hidden"
					id="userID" name="userID" value="UID101"> <input
					type="hidden" id="clientType" name="clientType" value="B2B">
				<input type="hidden" id="clientID" name="clientID" value="B2B101">
				
				</div>
			</form>
			<!--Banner Search Sec - Ends-->


		</div>
	</div>
</body>
</html>
