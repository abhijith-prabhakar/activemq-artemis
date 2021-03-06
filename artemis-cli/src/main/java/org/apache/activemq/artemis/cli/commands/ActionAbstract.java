/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.artemis.cli.commands;

import java.io.File;

import io.airlift.airline.Option;

public abstract class ActionAbstract implements Action {

   @Option(name = "--verbose", description = "Adds more information on the execution")
   boolean verbose;

   private String brokerInstance;

   private String brokerHome;

   protected ActionContext context;

   @Override
   public boolean isVerbose() {
      return verbose;

   }

   @Override
   public void setHomeValues(File brokerHome, File brokerInstance) {
      if (brokerHome != null) {
         this.brokerHome = brokerHome.getAbsolutePath();
      }
      if (brokerInstance != null) {
         this.brokerInstance = brokerInstance.getAbsolutePath();
      }
   }

   @Override
   public String getBrokerInstance() {
      if (brokerInstance == null) {
         /* We use File URI for locating files.  The ARTEMIS_HOME variable is used to determine file paths.  For Windows
         the ARTEMIS_HOME variable will include back slashes (An invalid file URI character path separator).  For this
         reason we overwrite the ARTEMIS_HOME variable with backslashes replaced with forward slashes. */
         brokerInstance = System.getProperty("artemis.instance");
         if (brokerInstance != null) {
            brokerInstance = brokerInstance.replace("\\", "/");
            System.setProperty("artemis.instance", brokerInstance);
         }
      }
      return brokerInstance;
   }

   @Override
   public String getBrokerHome() {
      if (brokerHome == null) {
         /* We use File URI for locating files.  The ARTEMIS_HOME variable is used to determine file paths.  For Windows
         the ARTEMIS_HOME variable will include back slashes (An invalid file URI character path separator).  For this
         reason we overwrite the ARTEMIS_HOME variable with backslashes replaced with forward slashes. */
         brokerHome = System.getProperty("artemis.home");
         if (brokerHome != null) {
            brokerHome = brokerHome.replace("\\", "/");
            System.setProperty("artemis.home", brokerHome);
         }

         if (brokerHome == null) {
            // if still null we will try to improvise with "."
            brokerHome = ".";
         }
      }
      return brokerHome;
   }

   @Override
   public Object execute(ActionContext context) throws Exception {
      this.context = context;

      return null;
   }

}
