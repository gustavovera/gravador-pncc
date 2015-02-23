var cordova = require('cordova'),
    channel = require('cordova/channel'),
    exec = require('cordova/exec');

exports.start = function (name, callback, error) {
	
	exec(callback, error,'GravadorPNCC','start',[name]);
};

exports.stop = function (callback, error) {
	
	exec(callback, error,'GravadorPNCC','stop',[]);
};
