'use strict';

function safeApply(scope, fn) {
    (scope.$$phase || scope.$root.$$phase) ? fn() : scope.$apply(fn);
}

function removeInArr(arr, item) {
      for(var i = arr.length; i--;) {
          if(arr[i] === item) {
              arr.splice(i, 1);
          }
      }
  }

Object.size = function(obj) {
    var size = 0, key;
    for (key in obj) {
        if (obj.hasOwnProperty(key)) size++;
    }
    return size;
};

if (typeof String.prototype.startsWith != 'function') {
  // see below for better implementation!
  String.prototype.startsWith = function (str){
    return this.indexOf(str) == 0;
  };
}

function in_array(needle, haystack, argStrict) {
  //  discuss at: http://phpjs.org/functions/in_array/
  // original by: Kevin van Zonneveld (http://kevin.vanzonneveld.net)
  // improved by: vlado houba
  // improved by: Jonas Sciangula Street (Joni2Back)
  //    input by: Billy
  // bugfixed by: Brett Zamir (http://brett-zamir.me)
  //   example 1: in_array('van', ['Kevin', 'van', 'Zonneveld']);
  //   returns 1: true
  //   example 2: in_array('vlado', {0: 'Kevin', vlado: 'van', 1: 'Zonneveld'});
  //   returns 2: false
  //   example 3: in_array(1, ['1', '2', '3']);
  //   example 3: in_array(1, ['1', '2', '3'], false);
  //   returns 3: true
  //   returns 3: true
  //   example 4: in_array(1, ['1', '2', '3'], true);
  //   returns 4: false

  var key = '',
    strict = !! argStrict;

  //we prevent the double check (strict && arr[key] === ndl) || (!strict && arr[key] == ndl)
  //in just one for, in order to improve the performance 
  //deciding wich type of comparation will do before walk array
  if (strict) {
    for (key in haystack) {
      if (haystack[key] === needle) {
        return true;
      }
    }
  } else {
    for (key in haystack) {
      if (haystack[key] == needle) {
        return true;
      }
    }
  }

  return false;
}

/* App Module */

var App = angular.module('App', [
  'ngRoute',
  'ngSanitize',
  'appControllers',
  'appFilters',
  'appServices',
  'appDirectives',
  'summernote',
  'ngFacebook'
]);

App.config(['$routeProvider', '$facebookProvider', '$locationProvider', '$httpProvider', function($routeProvider, $facebookProvider, $locationProvider, $httpProvider) {
  $facebookProvider.setAppId('385886264899275');  

  $routeProvider.
    when('/', {
      templateUrl: 'partials/feed.html',
      controller: 'FeedController'
    }).
    when('/questions/:questionAlias', {
      templateUrl: 'partials/question.html',
      controller: 'QuestionController'
    }).
    when('/topics/:topicAlias', {
      templateUrl: 'partials/topic-questions.html',
      controller: 'TopicQuestionController'
    }).
    when('/users/:userAlias', {
      templateUrl: 'partials/user-questions.html',
      controller: 'UserQuestionController'
    }).
    when('/search', {
      templateUrl: 'partials/search-content.html',
      controller: 'SearchContentCtrl'
    }).
    when('/profile', {
      templateUrl: 'partials/profile.html',
      controller: 'UserProfileController'
    }).
    when('/admin', {
      templateUrl: 'partials/admin.html',
      controller: 'AdminCtrl'
    }).
    when('/bookmark', {
      templateUrl: 'partials/bookmark.html',
      controller: 'BookmarkCtrl'
    }).
    otherwise({
      redirectTo: '/'
    });
    $locationProvider.html5Mode(true);
    $locationProvider.hashPrefix('!');
    $httpProvider.interceptors.push('errorInterceptor');
}]);

App.run(function($rootScope) {
  (function(){
     // If we've already installed the SDK, we're done
     if (document.getElementById('facebook-jssdk')) {return;}

     // Get the first script element, which we'll use to find the parent node
     var firstScriptElement = document.getElementsByTagName('script')[0];

     // Create a new script element and set its id
     var facebookJS = document.createElement('script'); 
     facebookJS.id = 'facebook-jssdk';

     // Set the new script's source to the source of the Facebook JS SDK
     facebookJS.src = '//connect.facebook.net/en_US/all.js';

     // Insert the Facebook JS SDK into the DOM
     firstScriptElement.parentNode.insertBefore(facebookJS, firstScriptElement);
   }());

  $rootScope.objectSize = function(obj) {
    var size = 0, key;
    for (key in obj) {
        if (obj.hasOwnProperty(key) && typeof(obj[key]) == 'string') {
          if(!key.startsWith("$")) size++; 
        }
    }
    return size;
  }
})
