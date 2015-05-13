'use strict';

/* Services */

var appServices = angular.module('appServices', ['ngResource']);
var port = 9001;
var url = 'http://backend.com';

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
			return $resource(url + '/oauth2/access_token', {client_id: 'temp', grant_type: 'password', oauth_token: $.cookie('user-accesstoken')}, {
				'signIn' : { method: 'GET'},
				'signOut' : { method: 'DELETE' }
			})
		},
		content : function(){
			return $resource(url + '/admin/api/contents/:id', {id: '@id', oauth_token: $.cookie('user-accesstoken')}, {
				'create': {method: 'POST'},
				'update': {method: 'PUT'},
				'delete': {method: 'DELETE'},
				'list': {method: 'GET'}
			});
		},
		comment : function(){
			return $resource(url + '/admin/api/comments/:id', {id: '@id', oauth_token: $.cookie('user-accesstoken')}, {
				'create': {method: 'POST'},
				'update': {method: 'PUT'},
				'delete': {method: 'DELETE'},
				'list': {method: 'GET'}
			});
		},
		tag : function(){
			return $resource(url + '/admin/api/tags/:id', {id: '@id', oauth_token: $.cookie('user-accesstoken')}, {
				'create': {method: 'POST'},
				'update': {method: 'PUT'},
				'delete': {method: 'DELETE'},
				'list': {method: 'GET'}
			});
		},
		hub : function(){
			return $resource(url + '/admin/api/hubs?token=:tokenId', {id: '@id', tokenId: '@tokenId', oauth_token: $.cookie('user-accesstoken')}, {
				'create': {method: 'POST'},
				'update': {method: 'PUT'},
				'delete': {method: 'DELETE'},
				'list': {method: 'GET'}
			});
		}
		//APPEND HERE
	}
}]);

appServices.factory('UserInfo', ['$rootScope', 'Resources', function($rootScope, Resources, authService){
	return {
		login: function(info) {
			Resources['auth']().signIn(info, function(data){
		  		$.cookie('user-accesstoken', data.access_token);
		  		$.cookie('user-refreshtoken', data.refresh_token);
		  		$.cookie('user-data', angular.toJson(data.user));

		  		$rootScope.$emit('userLogin');
		  	})
		},
		logout: function() {
			Resources['auth']().signOut({}, function(data) {
				$.removeCookie('user-accesstoken');
				$rootScope.$emit('userLogout');
			})
		}
	}
}]);

appServices.factory('ContentService', ['$rootScope', 'Resources', function($rootScope, Resources){
	var getList = function(view) {
		var params = {
			start: view.start, 
			size: view.size, 
			sort: view.sort, 
			sortOrder: view.sortOrder	
		}

		if(view.field != undefined && view.fieldValue != undefined) {
			params['field'] = view.field;
			params['fieldValue'] = view.fieldValue;
		}

	 	Resources['content']().list(params, function(response) {
			view.total = response.total;
			view.items = response.items;
			var pageNumber = Math.floor(response.total / view.size)
			var rem = response.total % view.size;

			if(rem > 0) view.pageNumber = pageNumber + 1; else view.pageNumber = pageNumber;
	 	}, function(errors) {
	 		console.log("errors: ", errors);
	 	})
	}

	return {
	 	delete: function(id, view) {
	 		Resources['group']().delete({id: id}, function(response) {
	 			getList(view);
	 		})
	 	},
		create: function(input, view) {
			if(input.group.id != undefined) {
				Resources['group']().update(input.group, function(response) {
					getList(view);
					input.group = {};
				}, function(errors) {
					console.log("errors: ", errors);
				})
			}
			else {
				Resources['group']().create(input.group, function(response) {
					getList(view);
				}, function(errors) {
					console.log("errors: ", errors);
				})	
			}
		},
		reset: function(input) {
			input.group = {};
		},
		prepareUpdate: function(item, input) {
			input.group = item;
		},
	 	goPage: function(i, view) {
	 		view.start = (i) * view.size;
	 		view.page = i;
	 		getList(view);
	 	},
	 	isSortable: function(name, filterFields) {
	 		return in_array(name, filterFields);
	 	},
	 	sort: function(field, view) {
	 		if(view.sort == field) {
	 			if(view.sortOrder == 'ASC') 
	 				view.sortOrder= 'DESC';
	 			else view.sortOrder = 'ASC';
	 		}
	 		else {
	 			view.sort = field;
	 			view.sortOrder = "ASC";
	 		}

	 		getList(view);
	 	},
	 	getList: getList,
	 	getMatch: function(view, field, value) {
	 	  view.field = field;
	 	  view.fieldValue = value;
	 	  getList(view);
	 	}
	}
}]);

appServices.factory('CommentService', ['$rootScope', 'Resources', function($rootScope, Resources){
	var getList = function(view) {
	 	Resources['comment']().list({start: view.start, size: view.size, sort: view.sort, sortOrder: view.sortOrder	}, function(response) {
			view.total = response.total;
			view.items = response.items;
			var pageNumber = Math.floor(response.total / view.size)
			var rem = response.total % view.size;

			if(rem > 0) view.pageNumber = pageNumber + 1; else view.pageNumber = pageNumber;
	 	}, function(errors) {
	 		console.log("errors: ", errors);
	 	})
	}

	return {
	 	delete: function(id, view) {
	 		Resources['comment']().delete({id: id}, function(response) {
	 			getList(view);
	 		})
	 	},
	 	goPage: function(i, view) {
	 		view.start = (i) * view.size;
	 		view.page = i;
	 		getList(view);
	 	},
	 	isSortable: function(name, filterFields) {
	 		return in_array(name, filterFields);
	 	},
	 	sort: function(field, view) {
	 		if(view.filter == field) {
	 			if(view.direction == 'ASC') 
	 				view.direction = 'DESC';
	 			else view.direction = 'ASC';
	 		}
	 		else {
	 			view.filter = field;
	 			view.direction = "ASC";
	 		}

	 		getList(view);
	 	},
	 	getList: getList
	}
}]);

appServices.factory('TagService', ['$rootScope', 'Resources', function($rootScope, Resources){
	var getList = function(view) {
	 	Resources['tag']().list({start: view.start, size: view.size, sort: view.sort, sortOrder: view.sortOrder	}, function(response) {
			view.total = response.total;
			view.items = response.items;
			var pageNumber = Math.floor(response.total / view.size)
			var rem = response.total % view.size;

			if(rem > 0) view.pageNumber = pageNumber + 1; else view.pageNumber = pageNumber;
	 	}, function(errors) {
	 		console.log("errors: ", errors);
	 	})
	}

	return {
	 	delete: function(id, view) {
	 		Resources['tag']().delete({id: id}, function(response) {
	 			getList(view);
	 		})
	 	},
		create: function(input, view) {
			if(input.tag.id != undefined) {
				Resources['tag']().update(input.tag, function(response) {
					getList(view);
					input.tag = {};
				}, function(errors) {
					console.log("errors: ", errors);
				})
			}
			else {
				Resources['tag']().create(input.tag, function(response) {
					getList(view);
				}, function(errors) {
					console.log("errors: ", errors);
				})	
			}
		},
		reset: function(input) {
			input.tag = {};
		},
		prepareUpdate: function(item, input) {
			input.tag = item;
		},
	 	goPage: function(i, view) {
	 		view.start = (i) * view.size;
	 		view.page = i;
	 		getList(view);
	 	},
	 	isSortable: function(name, filterFields) {
	 		return in_array(name, filterFields);
	 	},
	 	sort: function(field, view) {
	 		if(view.filter == field) {
	 			if(view.direction == 'ASC') 
	 				view.direction = 'DESC';
	 			else view.direction = 'ASC';
	 		}
	 		else {
	 			view.filter = field;
	 			view.direction = "ASC";
	 		}

	 		getList(view);
	 	},
	 	getList: getList
	}
}]);