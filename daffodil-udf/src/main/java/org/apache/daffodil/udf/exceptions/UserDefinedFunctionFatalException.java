/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.daffodil.udf.exceptions;

/**
 * Exception to throw to abort parsing/unparsing.
 */
@SuppressWarnings("serial")
public class UserDefinedFunctionFatalException extends Exception {

  /**
   * Constructs a new exception with a specified detail message
   *
   * @param errorMessage the detail message
   */
  public UserDefinedFunctionFatalException(String errorMessage) {
    super(errorMessage);
  }

  /**
   * Constructs a new exception with a specified cause
   *
   * @param cause the cause of the exception
   */
  public UserDefinedFunctionFatalException(Throwable cause) {
    super(cause);
  }
}
