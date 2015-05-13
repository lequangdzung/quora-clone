'use strict';

/* Services */

var appServices = angular.module('appServices', ['ngResource']);
var port = 9001;
var url = 'http://backend.com';

var cookieOptions = { domain: '.contentalk.com', path: '/' }
//var url = 'http://localhost:' + port;

appServices.factory('httpBuffer', ['$injector', function($injector) {
  /** Holds all the requests, so they can be re-requested in future. */
  var buffer = [];

  /** Service initialized later because of circular dependency problem. */
  var $http;

  function retryHttpRequest(config, deferred) {
    function successCallback(response) {
      deferred.resolve(response);
    }
    function errorCallback(response) {
      deferred.reject(response);
    }
    $http = $http || $injector.get('$http');
    $http(config).then(successCallback, errorCallback);
  }

  return {
    /**
     * Appends HTTP request configuration object with deferred response attached to buffer.
     */
    append: function(config, deferred) {
      buffer.push({
        config: config,
        deferred: deferred
      });
    },

    /**
     * Abandon or reject (if reason provided) all the buffered requests.
     */
    rejectAll: function(reason) {
      if (reason) {
        for (var i in buffer) {
          buffer[i].deferred.reject(reason);
        }
      }
      buffer = [];
    },

    /**
     * Retries all the buffered requests clears the buffer.
     */
    retryAll: function(updater) {
      for (var i in buffer) {
        retryHttpRequest(updater(buffer[i].config), buffer[i].deferred);
        console.log("buffer config", buffer[i].config);
      }
      buffer = [];
    }
  };
}]);

appServices.factory('authService', ['$rootScope','httpBuffer', function($rootScope, httpBuffer) {
  return {
    /**
     * Call this function to indicate that authentication was successfull and trigger a
     * retry of all deferred requests.
     * @param data an optional argument to pass on to $broadcast which may be useful for
     * example if you need to pass through details of the user that was logged in
     */
    loginConfirmed: function(data, configUpdater) {
      var updater = configUpdater || function(config) {return config;};
      $rootScope.$broadcast('event:auth-loginConfirmed', data);
      httpBuffer.retryAll(updater);
    },

    /**
     * Call this function to indicate that authentication should not proceed.
     * All deferred requests will be abandoned or rejected (if reason is provided).
     * @param data an optional argument to pass on to $broadcast.
     * @param reason if provided, the requests are rejected; abandoned otherwise.
     */
    loginCancelled: function(data, reason) {
      httpBuffer.rejectAll(reason);
      $rootScope.$broadcast('event:auth-loginCancelled', data);
    }
  };
}])

appServices.factory('errorInterceptor', ['$q', '$rootScope', '$location', 'httpBuffer',
    function ($q, $rootScope, $location, httpBuffer) {
        return {
            request: function (config) {
                return config || $q.when(config);
            },
            requestError: function(request){
                return $q.reject(request);
            },
            response: function (response) {
                return response || $q.when(response);
            },
            responseError: function (response) {
                if (response && response.status === 404) {
                }
                if (response && response.status === 401) {
                	var deferred = $q.defer();
                	httpBuffer.append(response.config, deferred);
                	$rootScope.$broadcast('event:auth-loginRequired', response);
                }
                if (response && response.status >= 500) {
                }
                return $q.reject(response);
            }
        };
}]);

appServices.factory('Resources', ['$resource', function($resource){
	return {
		auth: function() {
			return $resource(url + '/oauth2/access_token', { client_id: 'temp', grant_type: 'password', oauth_token: $.cookie('user-accesstoken')}, {
				'signIn' : { method: 'GET' },
				'signOut' : { method: 'DELETE' }
			})
		},
		password: function() {
			return $resource(url + '/api/users', { client_id: 'temp', grant_type: 'password', oauth_token: $.cookie('user-accesstoken')}, {
				'update' : { method: 'PUT' }
			})
		},
		authfb: function() {
			return $resource(url + '/oauth2/fb/login', {}, {
				'signIn' : { method: 'PUT'},
				'signOut' : { method: 'DELETE' }
			})
		},
		notify: function() {
			return $resource(url + '/api/users/notify', { client_id: 'temp', grant_type: 'password', oauth_token: $.cookie('user-accesstoken')}, {
				'getNotifications' : { method: 'GET', isArray: true }
			})
		},
		//auth: $resource(url + '/oauth2/access_token', { client_id: 'temp', grant_type: 'password' }),
		user: $resource(url + '/api/users/:userId', {userId: '@id'})
	}

}]);


appServices.factory('UI', ['$rootScope', function($rootScope){
	return {
		backdrop: function(target, type) {
			var objTarget = $(target);
			objTarget.css('position', 'relative').css('width', '100%');
			objTarget.css({'z-index': 1050});
			var bd = $('<div class="modal-backdrop in' +  '" />').appendTo($('body'));
			objTarget.on('click', $.proxy(function (e) {
			          if (e.target !== e.currentTarget) return;
			          type == undefined
			            ? objTarget.focus()
			            : type()
			        }, this));
		},
		cancelBackdrop: function(target, type) {
			var objTarget = $(target);
			objTarget.css('position', 'static').css('width', 'auto');
			objTarget.css({'z-index': 0});
			$('.modal-backdrop').remove();
		}
	}
}]);


appServices.factory('MediaUtil', function($http) {
  var resize = function(sDataURL, maxWidth, maxHeight) {
  	var canvas = document.createElement('canvas');

  	var img = document.createElement("img");
  	img.src = sDataURL;

  	var ctx = canvas.getContext("2d");
  	ctx.drawImage(img, 0, 0);

  	var MAX_WIDTH = maxWidth;
  	var MAX_HEIGHT = maxHeight;
  	var width = img.width;
  	var height = img.height;

  	if (width > height) {
  	  if (width > MAX_WIDTH) {
  	    height *= MAX_WIDTH / width;
  	    width = MAX_WIDTH;
  	  }
  	} else {
  	  if (height > MAX_HEIGHT) {
  	    width *= MAX_HEIGHT / height;
  	    height = MAX_HEIGHT;
  	  }
  	}
  	canvas.width = width;
  	canvas.height = height;
  	var ctx = canvas.getContext("2d");
  	ctx.drawImage(img, 0, 0, width, height);

  	var dataurl = canvas.toDataURL("image/png");
  	return dataurl;
  }

  var getVideoThumb = function(provider, videoId) {
  	var thumbUrl = '';
  	if(provider == 'youtube') {
  		thumbUrl = "http://img.youtube.com/vi/"+videoId+"/hqdefault.jpg"
  	}
  	else if(provider == 'vimeo'){
  	  $http.get('http://vimeo.com/api/v2/video/'+videoId+'.json').then(function(response){
  	  	thumbUrl = response.data[0]['thumbnail_medium'];
  	  })
  	}
  	else if(provider == 'dailymotion') {
  		thumbUrl = ' http://www.dailymotion.com/thumbnail/video/' + videoId;
  	}

  	return thumbUrl;
  }

  return {
  	resizeImage: resize,
  	getVideoThumb: getVideoThumb
  }
});


appServices.factory('UserInfo', ['$rootScope', 'Resources', 'authService', '$facebook', function($rootScope, Resources, authService, $facebook){
	var isAutoLogin = false;

	var isLoggedIn = function() {
		return !angular.isUndefined($.cookie('user-accesstoken')) && $.cookie('user-accesstoken') != ""
	}

	return {
		isLoggedIn: isLoggedIn,
		getAccessToken: function() {
			return $.cookie('user-accesstoken');
		},
		getUser: function() {
			var user = angular.fromJson($.cookie('user-data'));	
			return user;	
		},
		follow: function(target, target_id) {
			var user = angular.fromJson($.cookie('user-data'));	
			if(user.followings[target] == undefined) {
				user.followings[target] = [target_id];
			}
			else
				user.followings[target].push(target_id);
			$.cookie('user-data', angular.toJson(user), cookieOptions);
		},
		unfollow: function(target, target_id) {
			var user = angular.fromJson($.cookie('user-data'));	
			removeInArr(user.followings[target], target_id);
			$.cookie('user-data', angular.toJson(user), cookieOptions);
		},
		addTag: function(tagId) {
			var user = angular.fromJson($.cookie('user-data'));	
			user.tags.push(tagId);
			$.cookie('user-data', angular.toJson(user), cookieOptions);
		},
		delTag: function(tagId) {
			var user = angular.fromJson($.cookie('user-data'));	
			removeInArr(user.tags, tagId);
			$.cookie('user-data', angular.toJson(user), cookieOptions);
		},
		addFollow: function(userId) {
			var user = angular.fromJson($.cookie('user-data'));	
			user.followings.push(userId);
			$.cookie('user-data', angular.toJson(user), cookieOptions);
		},
		delFollow: function(userId) {
			var user = angular.fromJson($.cookie('user-data'));	
			removeInArr(user.followings, userId);
			$.cookie('user-data', angular.toJson(user), cookieOptions);
		},
		resetNotify: function() {
			var user = angular.fromJson($.cookie('user-data'));	
			user.unread = [];
			$.cookie('user-data', angular.toJson(user), cookieOptions);
		},
		addNotify: function(act) {
			var user = angular.fromJson($.cookie('user-data'));	
			user.unread.push(act);
			$.cookie('user-data', angular.toJson(user), cookieOptions);
		},
		register: function(user) {
			Resources['user'].save(user, function(data){
		  		$.cookie('user-accesstoken', data.access_token, cookieOptions);
		  		$.cookie('user-refreshtoken', data.refresh_token, cookieOptions);
		  		$.cookie('user-data', angular.toJson(data.user), cookieOptions);

		  		$rootScope.$emit('userLogin');
		      	authService.loginConfirmed(null, function(config) {
			        if(angular.isUndefined(config.params)) config.params = {};
			        config.params['oauth_token'] = data.access_token;
			        return config;
		      	});
		  	})
		},
		login: function(info) {
			Resources['auth']().signIn(info, function(data){
		  		$.cookie('user-accesstoken', data.access_token, cookieOptions);
		  		$.cookie('user-refreshtoken', data.refresh_token, cookieOptions);
		  		$.cookie('user-data', angular.toJson(data.user), cookieOptions);

		  		$rootScope.$emit('userLogin', data);
		      	authService.loginConfirmed(null, function(config) {
			        if(angular.isUndefined(config.params)) config.params = {};
			        config.params['oauth_token'] = data.access_token;
			        return config;
		      	});
		  	})

			//$('#myModal').modal('toggle');
		},
		update_password: function(info) {
			Resources['password']().update(info, function(data){
		  	$rootScope.$emit('passwordUpdated');
		  	var user = angular.fromJson($.cookie('user-data'));	
		  	user.required_pass = false;
				$.cookie('user-data', angular.toJson(user), cookieOptions);		  			
		  }, function(error){
		  	console.log("erros is: ", error);
		  })

			//$('#myModal').modal('toggle');
		},
		fblogin: function() {
			$facebook.login().then(function(response) {
			  var token = response.authResponse.accessToken;
			  console.log("token is: ", token);
			  Resources.authfb(token).signIn({token: token}, function(data){
			  	$.cookie('user-accesstoken', data.access_token, cookieOptions);
			  	$.cookie('user-refreshtoken', data.refresh_token, cookieOptions);
			  	$.cookie('user-data', angular.toJson(data.user), cookieOptions);

  	  		$rootScope.$emit('userLogin', data);
	      	authService.loginConfirmed(null, function(config) {
		        if(angular.isUndefined(config.params)) config.params = {};
		        config.params['oauth_token'] = data.access_token;
		        return config;
	      	});
			  })  
			});
		},
		fbautologin: function() {
			if(!isLoggedIn()) {
				isAutoLogin = true;
				$facebook.getLoginStatus().then(function(response){
					console.log("response fbautologin: ", response);
					if(response.authResponse != undefined) {
						var token = response.authResponse.accessToken;
						Resources.authfb(token).signIn({token: token}, function(data){
							$.cookie('user-accesstoken', data.access_token);
							$.cookie('user-refreshtoken', data.refresh_token);
							$.cookie('user-data', angular.toJson(data.user));

							$rootScope.$emit('userLogin');
						  	authService.loginConfirmed(null, function(config) {
						        if(angular.isUndefined(config.params)) config.params = {};
						        config.params['oauth_token'] = data.access_token;
						        return config;
						  	});
						})  
					}
					else isAutoLogin = false;
				});
			}
		},
		logout: function() {
			Resources['auth']().signOut({}, function(data) {
				$.removeCookie('user-accesstoken', cookieOptions);
				$.removeCookie('user-data', cookieOptions);
				$rootScope.$emit('userLogout', []);
			})
		},
		isAutoLogging: function() { 
			console.log("isAutoLogin: ", isAutoLogin);
			return isAutoLogin;
		}
	}
}]);

appServices.factory('$userfeed', ['$rootScope', '$timeout', '$resource', 'UserInfo', function($rootScope, $timeout, $resource, UserInfo){
	$rootScope.$on('userLogin', function(event) {
	  User = UserInfo.getUser();
	});

	var User = UserInfo.getUser();

	
	var isFollowed = function(target, target_id) {
		if(User == undefined) return false;
	  return in_array(target_id, User[target]);
	}

	var isOwner = function(userId) {
  	if(User != undefined) return (userId == User.id);
		else return false;
	}
  
  var show_target = function(target) {
    if(User != undefined) {
      if(target.id == User.id) {
        return "bạn";
      }
      else return target.display;
    }
		else return target.display;
  }
  
  var show_action = function(act) {
    if(act == 'ask') return 'hỏi';
    else if(act=='answer') return 'trả lời';
    else if(act =='voteup') return 'thấy hay';
    else return '';
  }
  
	var resource = $resource(url + '/api/users/follow/:target/:target_id', {target: '@target', target_id: '@target_id', oauth_token: $.cookie('user-accesstoken')}, {
    'follow': {method: 'POST', isArray: false},
    'unfollow': {method: 'DELETE', isArray: false}
  });

  var commentResource = $resource(url + '/api/comments/:target/:target_id', {target: '@target', target_id: '@target_id', oauth_token: $.cookie('user-accesstoken')}, {
    'getComments': {method: 'GET', isArray: true},
    'comment': {method: 'POST', isArray: false}
  });

  var show_comment = function(target, target_id, obj) {
  	commentResource.getComments(
  		{target: target, target_id: target_id},
  		function(response){
  			obj.comments = response;
        obj.show_comment = true;
  		}
  	);
  }

  var add_comment = function(target, target_id, obj, body) {
  	commentResource.comment(
  		{target: target, target_id: target_id, body: body},
  		function(response){
  			obj.comments.unshift(response);
        
  		}
  	);
  }

	return {
		follow: function(target, target_id, obj){
			resource.follow({target: (target + 's'), target_id: target_id}, function(response){
				UserInfo.follow(target, target_id);
				obj.followerCount = obj.followerCount + 1;
				if(obj.actions != undefined) {
					obj.actions['follow'] = 'Y';
				}
			})
		},
    follows: function(target, target_ids){
			resource.follow({target: (target + 's'), ids: target_ids}, function(response){

			})
		},
		unfollow: function(target, target_id, obj) {
			bootbox.confirm("Are you sure want to remove this " + target + "?", function(result) {
				resource.unfollow({target: (target + 's'), target_id: target_id}, function(response){
					UserInfo.unfollow(target, target_id);
					obj.followerCount = obj.followerCount - 1;
					if(obj.actions != undefined) {
						delete obj.actions['follow'];
					}
				})	
			})
		},
		show_comment: show_comment,
		add_comment: add_comment,
		isFollowed: isFollowed,
		isOwner: isOwner,
    show_target: show_target,
    show_action: show_action
	}
}]);
