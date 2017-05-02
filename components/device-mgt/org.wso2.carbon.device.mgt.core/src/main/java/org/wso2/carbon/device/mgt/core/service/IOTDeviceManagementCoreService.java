/*
 * Copyright 2005-2015 WSO2, Inc. (http://wso2.com)
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
package org.wso2.carbon.device.mgt.core.service;

import org.wso2.carbon.device.mgt.common.DeviceManagementException;
import org.wso2.carbon.device.mgt.common.iot.IOTDevice;
import org.wso2.carbon.device.mgt.common.iot.IOTDeviceType;
import org.wso2.carbon.device.mgt.common.iot.IOTOperation;

import java.util.List;

public interface IOTDeviceManagementCoreService {

    List<String> getIoTDeviceTypeNames() throws DeviceManagementException;

    void addIoTDeviceType(IOTDeviceType deviceTypeInfo) throws DeviceManagementException;

    IOTDevice addIoTDevice(IOTDevice device) throws DeviceManagementException;

    List<IOTDevice> getIoTDevices() throws DeviceManagementException;

    IOTDevice getIoTDevice(String identifier) throws DeviceManagementException;

    void addOperation(IOTOperation operation) throws DeviceManagementException;

    List<IOTOperation> getOperations(String deviceIdentifier) throws DeviceManagementException;
}
