'use strict';

/* Controllers */

var appControllers = angular.module('appControllers', []);

appControllers.controller('NotFoundController', ['$scope', '$rootScope', '$location', 'Resources', 'UserInfo', function($scope, $rootScope, $location, Resources, UserInfo) {
  
}]);

appControllers.controller('NavbarCtrl', ['$scope', '$location', '$rootScope', '$routeParams', 'UserInfo', '$resource', function($scope, $location, $rootScope, $routeParams, UserInfo, $resource) {
  $scope.login = {};
  $scope.words = {
    action: {
      'answer': 'trả lời',
      'comment': 'bình luận'
    },
    ob: {
      'question': 'câu hỏi',
      'answer': 'câu trả lời'
    }
  }

  var resource = $resource(url + '/api/users/:suffix', {suffix: '@suffix', oauth_token: $.cookie('user-accesstoken')}, {
    'unread': {method: 'GET', params: {suffix: 'unread'}, isArray: false},
    'notify': {method: 'GET', params: {suffix: 'notify'}, isArray: true}
  });

  var resetUser = function() {
    safeApply($scope, function(){
      $scope.User = UserInfo['getUser']();
    }) 
  }

  var load = function() {
    $scope.isLoggedIn = UserInfo['isLoggedIn']();
    resetUser();
    if($scope.isLoggedIn) {
      resource.unread({}, function(response) {
        $scope.unread = response.unread;
        console.log("unread response: ", response);
      })
    }
  }

  $rootScope.$on('userLogin', function(event, labels) {
    load();
  });

  $rootScope.$on('userLogout', function(event, labels) {
    load();
  });

  load();


  $scope.ACT = {
    signIn: function() {
      $('#authForm').modal({show: true, backdrop: 'static'});
      $rootScope.$broadcast('startShowLoginForm');
    },
    signOut: function() {
      UserInfo['logout']();
    },
    getNotifications: function() {
      resource.notify({}, function(response){
        $scope.notifications = response;
      })
    },
    askForm: function() {
      $('#askForm').modal({show: true, backdrop: 'static'});
    }
  }
}]);

appControllers.controller('QuestionFormController', ['$scope', '$resource', '$rootScope', '$routeParams', 'UserInfo', 'Resources', function($scope, $resource, $rootScope, $routeParams, UserInfo) {
  var resource = $resource(url + '/api/questions/:suffix', {suffix: '@suffix', oauth_token: $.cookie('user-accesstoken')}, {
    'confirm': {method: 'POST', params: {suffix: 'confirm'}, isArray: true},
    'create': {method: 'POST', isArray: false}
  });

  var topicResource = $resource(url + '/api/topics/:suffix', {suffix: '@suffix', oauth_token: $.cookie('user-accesstoken')}, {
    'search': {method: 'POST', params: {suffix: 'search'}, isArray: false}
  });

  $scope.view = 'form'; //form, confirm, topic
  $scope.title = 'Câu hỏi';
  $scope.question = {};
  $scope.topic = {};
  $scope.search_results = [];
  $scope.suggests = [];
  $scope.selection = [];
  $scope.search_status = 0;
  $scope.ids = [];
  
  $scope.state = {back: [], current: 'form'};
  

  $scope.go_back = function() {
    $scope.state.current = $scope.state.back[$scope.state.back.length - 1]; 
    $scope.state.back.pop();
  }
  
  $scope.confirm = function() {
    resource.confirm($scope.question, function(response){
      if(response.length == 0) {
        $scope.get_suggest_topic();
      }
      else {
        $scope.state.back.push($scope.state.current);
        $scope.state.current = 'confirm';
        $scope.exist = response;
        $scope.title = "Kiểm tra đã có câu hỏi chưa";
      }
    })
  }

  $scope.get_suggest_topic = function() {
    topicResource.search({name: $scope.question.title}, function(response){
      $scope.state.back.push($scope.state.current);
      $scope.state.current = 'topic';
      $scope.title = "Thêm chủ đề";
    })
  }

  $scope.search_topic = function() {
    topicResource.search({name: $scope.topic.name, ids: $scope.ids}, function(response){
      if(response.topics.length == 0) {
        $scope.search_status = -1;
        $scope.search_results = [];
      }
      else {
        if(response.found) $scope.search_status = 0;
        else $scope.search_status = 1;
        $scope.search_results = response.topics;
      }
    })
  }

  $scope.add_topic = function() {
    $scope.suggests.push({name: $scope.topic.name});
    $scope.selection.push($scope.suggests.length - 1);
    $scope.topic.name = "";
    
  }
  
  $scope.add_exist_topic = function(topic) {
    $scope.suggests.push({name: topic.name, id: topic.id});
    $scope.selection.push($scope.suggests.length - 1);
    $scope.ids.push(topic.id);
    $scope.topic.name = "";
    $scope.search_results = [];
    $scope.search_status = 0;
  }

  $scope.add_question = function() {
    var topics = [];
    for (var index in $scope.selection) {
      topics.push($scope.suggests[index])
    }

    resource.create({title: $scope.question.title, topics: topics}, function(response){
      $('#askForm').modal('toggle');
    })
  }

  $scope.toggle_selection = function(index) {
    var idx = $scope.selection.indexOf(index);

    // is currently selected
    if (idx > -1) {
      $scope.selection.splice(idx, 1);
    }

    // is newly selected
    else {
      $scope.selection.push(index);
    }
  }
}]);

appControllers.controller('UserTopicController', ['$scope', '$route', '$location', '$resource', '$userfeed', function($scope, $route, $location, $resource, $userfeed) {
  var topicResource = $resource(url + '/api/topics/:suffix', {suffix: '@suffix', oauth_token: $.cookie('user-accesstoken')}, {
    'search': {method: 'POST', params: {suffix: 'search'}, isArray: false}
  });

  $scope.search_results = [];
  $scope.topics = [];
  $scope.search_status = 0;
  $scope.ids = [];
  
  
  $scope.search_topic = function() {
    topicResource.search({name: $scope.topic.name, ids: $scope.ids}, function(response){
      $scope.search_results = response.topics;
    })
  }

  $scope.follow = function(topic) {
    $scope.topics.push(topic);
    $scope.ids.push(topic.id);
    $scope.search_results = [];
    $scope.topic.name = '';
  }
  
  $scope.go = function() {
    $userfeed.follows('topic', $scope.ids);
  }
}]);

appControllers.controller('FeedController', ['$scope', '$resource', '$userfeed', function($scope, $resource, $userfeed) {
  $scope.userfeed = $userfeed;
  $scope.filter = "hot";
  $scope.more = true;

  var resource = $resource(url + '/api/questions/:suffix', {suffix: '@suffix', oauth_token: $.cookie('user-accesstoken')}, {
    'getList': {method: 'GET', isArray: true}
  });

  resource.getList({}, function(response){
    $scope.questions = response.slice(0, 5);
    if(response.length <= 5){
      $scope.more = false;
    }
    else $scope.more = true;
  
  })
  
  $scope.get_next = function() {
    var last = $scope.questions[$scope.questions.length -1];
    var last_id = last.id;
    var before = last.score;
    
    if($scope.filter == 'best') {
      before = last.confidence;
    }
    else if($scope.filter == 'new') {
      before = last.updated;
    }
    
    resource.getList({filter: $scope.filter, id: last_id, before: before}, function(response){
      if(response.length <= 5){
        $scope.more = false;
      }
      else $scope.more = true;
       
      $scope.questions = $scope.questions.concat(response.slice(0,5));
    })
  }

  $scope.afilter = function(a) {
    $scope.filter = a;
    resource.getList({filter: $scope.filter}, function(response){
      if(response.length <= 5){
        $scope.more = false;
      }
      else $scope.more = true;
       
      $scope.questions = response.slice(0, 5);
    })
  }
}]);

appControllers.controller('TopicQuestionController', ['$scope', '$routeParams', '$resource', '$userfeed', function($scope, $routeParams, $resource, $userfeed) {
  $scope.userfeed = $userfeed;
  $scope.filter = "hot";
  $scope.more = true;

  var resource = $resource(url + '/api/topics/:alias/questions', {alias: '@alias', oauth_token: $.cookie('user-accesstoken')}, {
    'getList': {method: 'GET', isArray: false}
  });

  resource.getList({alias: $routeParams.topicAlias}, function(response){
    $scope.questions = response.questions;
    if(response.questions.length <= 5){
      $scope.more = false;
    }
    else $scope.more = true;
    $scope.topic = response.topic;
  })


  $scope.afilter = function(a) {
    $scope.filter = a;
    resource.getList({alias: $routeParams.topicAlias, filter: $scope.filter}, function(response){
      if(response.questions.length <= 5){
        $scope.more = false;
      }
      else $scope.more = true;
       
      $scope.questions = response.questions.slice(0, 10);
    })
  }
}]);

appControllers.controller('UserQuestionController', ['$scope', '$routeParams', '$resource', '$userfeed', function($scope, $routeParams, $resource, $userfeed) {
  $scope.userfeed = $userfeed;
  $scope.filter = "hot";
  $scope.more = true;

  var resource = $resource(url + '/api/users/:alias/questions', {alias: '@alias', oauth_token: $.cookie('user-accesstoken')}, {
    'getList': {method: 'GET', isArray: false}
  });

  resource.getList({alias: $routeParams.userAlias}, function(response){
    $scope.questions = response.questions;
    if(response.questions.length <= 5){
      $scope.more = false;
    }
    else $scope.more = true;
    $scope.user = response.user;
  })

  $scope.afilter = function(a) {
    $scope.filter = a;
    resource.getList({alias: $routeParams.topicAlias, filter: $scope.filter}, function(response){
      if(response.questions.length <= 5){
        $scope.more = false;
      }
      else $scope.more = true;
       
      $scope.questions = response.questions.slice(0, 10);
    })
  }
}]);

appControllers.controller('UserProfileController', ['$scope', 'UserInfo', '$resource', '$userfeed', function($scope, UserInfo, $resource, $userfeed) {
  $scope.userfeed = $userfeed;
  $scope.filter = "hot";
  $scope.more = true;
  $scope.user = UserInfo.getUser();

  var resource = $resource(url + '/api/users/:alias/questions', {alias: '@alias', oauth_token: $.cookie('user-accesstoken')}, {
    'getList': {method: 'GET', isArray: false}
  });

  resource.getList({alias: $scope.user.alias}, function(response){
    $scope.questions = response.questions;
    if(response.questions.length <= 5){
      $scope.more = false;
    }
    else $scope.more = true;
  })

  $scope.afilter = function(a) {
    $scope.filter = a;
    resource.getList({alias: $routeParams.topicAlias, filter: $scope.filter}, function(response){
      if(response.questions.length <= 5){
        $scope.more = false;
      }
      else $scope.more = true;
       
      $scope.questions = response.questions.slice(0, 10);
    })
  }
}]);

appControllers.controller('QuestionController', ['$scope', '$routeParams', '$resource', '$userfeed', function($scope, $routeParams, $resource, $userfeed) {
  $scope.userfeed = $userfeed;

  $scope.input = {};
  $scope.filter = "hot";
  $scope.more = true;

  var resource = $resource(url + '/api/questions/:suffix', {suffix: '@suffix', oauth_token: $.cookie('user-accesstoken')}, {
    'getQuestion': {method: 'GET', isArray: false},
    'addAnswer': {method: 'POST', isArray: false}
  });

  var aResource = $resource(url + '/api/users/:type/:target/:target_id', {type: '@type', target: '@target', target_id: '@target_id', oauth_token: $.cookie('user-accesstoken')}, {
    'voteUp': {method: 'POST',params: {type: 'voteup'}, isArray: false},
    'deleteVoteUp': {method: 'DELETE',params: {type: 'voteup'}, isArray: false},
    'voteDown': {method: 'POST',params: {type: 'votedown'}, isArray: false},
    'deleteVoteDown': {method: 'DELETE',params: {type: 'votedown'}, isArray: false}
  });

  var answerResource = $resource(url + '/api/questions/:questionId/answers', {questionId: '@questionId', oauth_token: $.cookie('user-accesstoken')}, {
    'getAnswers': {method: 'GET', isArray: true}
  });

  resource.getQuestion({suffix: $routeParams.questionAlias}, function(response){
    if(response.question.answers.length <= 10){
      $scope.more = false;
    }
    
    $scope.question = response.question;
    $scope.question.answers = $scope.question.answers.slice(0, 10)
  })

  $scope.addAnswer = function() {
    resource.addAnswer({suffix: $scope.question.id, body: $scope.input.body}, function(response){
      $scope.question.answers.unshift(response);
      $scope.input.body = '';
    })
  }

  $scope.voteUp = function(target, target_id, action, obj) {
    if(action != 'up') {
      aResource.voteUp({target: target, target_id: target_id}, function(response){
        obj.voteUp = obj.voteUp + 1;
        obj.actions['vote'] = 'up';
        if(action =='down') obj.voteDown = obj.voteDown - 1;
      })
    }
    else {
      aResource.deleteVoteUp({target: target, target_id: target_id}, function(response){
        obj.voteUp = obj.voteUp - 1;
        delete obj.actions['vote'];
      })  
    }
  }

  $scope.voteDown = function(target, target_id, action, obj) {
    if(action != 'down') {
      aResource.voteDown({target: target, target_id: target_id}, function(response){
        obj.voteDown = obj.voteDown + 1;
        obj.actions['vote'] = 'down';
        if(action =='up') obj.voteUp = obj.voteUp - 1;
      })
    } else {
      aResource.deleteVoteDown({target: target, target_id: target_id}, function(response){
        obj.voteDown = obj.voteDown - 1;
        delete obj.actions['vote'];
      })
    }
  }

  $scope.afilter = function(a) {
    $scope.filter = a;
    answerResource.getAnswers({questionId: $scope.question.id, filter: $scope.filter}, function(response){
      if(response.length <= 10){
        $scope.more = false;
      }
      else $scope.more = true;
       
      $scope.question.answers = response.slice(0, 10);
    })
  }

  $scope.more = function() {
    var last = $scope.question.answers[9];
    var before = 0;
    if($scope.filter == 'top') before = (last.voteUp - last.voteDown);
    else if($scope.filter == 'new') before = last.updated;
    else before = last.confidence;

    answerResource.getAnswers({questionId: $scope.question.id, filter: $scope.filter, id: last.id, before: before}, function(response){
      $scope.question.answers = $scope.question.answers.concat(response);
      if(response.length <= 10) $scope.more = false;
    })
  }

  $scope.showAnswerForm = function() {
    $('.reply').css('display', 'block');
    $('.reply-front').css('display', 'none');
  }
}]);

appControllers.controller('authCtrl', ['$scope', '$rootScope', '$routeParams', 'UserInfo', 'Resources', '$facebook', function($scope, $rootScope, $routeParams, UserInfo, Resources, $facebook) {
  $scope.view = {
    form: 'social'
  }

  $scope.user = UserInfo['getUser']();

  if($scope.user != undefined && $scope.user.required_pass === true) {
    $scope.view.form = "password";
    $('#authForm').modal({show: true, backdrop: 'static'});
  }

  $scope.login = {};
  $scope.register = {};
  $scope.forgot = {};
  $scope.password = {};

  $rootScope.$on('event:auth-loginRequired', function() {
    $('#authForm').modal('show');
  });

  $rootScope.$on('event:auth-loginConfirmed', function() {
    //$('#authForm').modal('hide');
  });

  $rootScope.$on('userLogin', function(event, data) {
    if(data != undefined && data.required_pass === true) {
      $scope.user = UserInfo['getUser']();
      $scope.view.form = "password";
      $('#authForm').modal({show: true, backdrop: 'static'});
    }
    else {
      $('#authForm').modal('hide'); 
    }
  });

  $rootScope.$on('passwordUpdated', function(event) {
      $('#authForm').modal('hide');    
  });

  $scope.$on('startShowLoginForm', function(){
    $scope.view.form = 'social';
  })

  $scope.fblogin = UserInfo.fblogin;

  $scope.show = function(val) {
    safeApply($scope, function() {$scope.view.form = val});
  }

  $scope.registerUser = function() {
    UserInfo['register']($scope.register);
  }

  $scope.loginUser = function() {
  	// Need to get token and refersh token then store it to cookies
    UserInfo['login']($scope.login);
  }

  $scope.updatePassword = function() {
    // Need to get token and refersh token then store it to cookies
    UserInfo['update_password']($scope.password);
    $scope.user.required_pass= false;
  }
}]);
