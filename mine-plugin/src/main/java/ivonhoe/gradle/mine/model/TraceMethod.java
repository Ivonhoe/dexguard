/*
 * Tencent is pleased to support the open source community by making wechat-matrix available.
 * Copyright (C) 2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the BSD 3-Clause License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ivonhoe.gradle.mine.model;


import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import ivonhoe.gradle.increment.util.Utils;

/**
 * Created by caichongyang on 2017/6/3.
 */
public class TraceMethod {
    private static final String TAG = "Matrix.TraceMethod";
    public int id;
    public int accessFlag;
    public String className;
    public String methodName;
    public String desc;

    public static TraceMethod create(int id, int accessFlag, String className, String methodName, String desc) {
        TraceMethod traceMethod = new TraceMethod();
        traceMethod.id = id;
        traceMethod.accessFlag = accessFlag;
        traceMethod.className = className.replace("/", ".");
        traceMethod.methodName = methodName;
        traceMethod.desc = desc.replace("/", ".");
        return traceMethod;
    }

    public String getMethodName() {
        if (desc == null || isNativeMethod()) {
            return this.className + "." + this.methodName;
        } else {
            return this.className + "." + this.methodName + "." + desc;
        }
    }

    public String getReturn() {
        if (Utils.isNullOrNil(desc)) {
            return null;
        }
        return Type.getReturnType(desc).toString();
    }

    @Override
    public String toString() {
        if (desc == null || isNativeMethod()) {
            return id + "," + accessFlag + "," + className + " " + methodName;
        } else {
            return id + "," + accessFlag + "," + className + " " + methodName + " " + desc;
        }
    }

    public String toIgnoreString() {
        if (desc == null || isNativeMethod()) {
            return className + " " + methodName;
        } else {
            return className + " " + methodName + " " + desc;
        }
    }

    public boolean isNativeMethod() {
        return (accessFlag & Opcodes.ACC_NATIVE) != 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TraceMethod) {
            TraceMethod tm = (TraceMethod) obj;
            return tm.getMethodName().equals(getMethodName());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
