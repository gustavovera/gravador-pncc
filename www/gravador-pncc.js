var cordova = require('cordova'),
    channel = require('cordova/channel'),
    exec = require('cordova/exec');

exports.start = function (name, callback, error) {
	
	this.exec(callback, error,'PNCCPlugin','start',[name]);
};

exports.stop = function (callback, error) {
	
	this.exec(callback, error,'PNCCPlugin','stop',[]);
};
