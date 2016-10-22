/* 
 * Copyright 2014-2016 Samsung Research America, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.samsung.sjs.constraintsolver;

/**
 * An exception that indicates that no solution exists for a set of constraints.
 *
 * @author ftip
 *
 */
@SuppressWarnings("serial")
public class SolverException extends RuntimeException {
	public SolverException(String message){
		this.message = message;
	}

	public String getMessage(){
		return message;
	}

	protected String message;

	public String explanation() {
	    return message;
	}
}
