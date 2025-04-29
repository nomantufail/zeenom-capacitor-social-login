package com.zeenom.login.plugin;

import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "LoginPlugin")
public class LoginPluginPlugin extends Plugin {

  @Override
  public void load() {
    super.load();
  }

  PluginCall call;


  @PluginMethod
  public void echo(PluginCall call) {
  }

}
