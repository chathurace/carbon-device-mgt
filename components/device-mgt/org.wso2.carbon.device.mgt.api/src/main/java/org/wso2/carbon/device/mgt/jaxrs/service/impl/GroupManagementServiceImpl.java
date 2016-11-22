/*
 *   Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.wso2.carbon.device.mgt.jaxrs.service.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.device.mgt.common.Device;
import org.wso2.carbon.device.mgt.common.DeviceIdentifier;
import org.wso2.carbon.device.mgt.common.DeviceNotFoundException;
import org.wso2.carbon.device.mgt.common.GroupPaginationRequest;
import org.wso2.carbon.device.mgt.common.PaginationResult;
import org.wso2.carbon.device.mgt.common.group.mgt.DeviceGroup;
import org.wso2.carbon.device.mgt.common.group.mgt.GroupAlreadyExistException;
import org.wso2.carbon.device.mgt.common.group.mgt.GroupManagementException;
import org.wso2.carbon.device.mgt.common.group.mgt.GroupUser;
import org.wso2.carbon.device.mgt.common.group.mgt.RoleDoesNotExistException;
import org.wso2.carbon.device.mgt.core.service.GroupManagementProviderService;
import org.wso2.carbon.device.mgt.jaxrs.beans.*;
import org.wso2.carbon.device.mgt.jaxrs.service.api.GroupManagementService;
import org.wso2.carbon.device.mgt.jaxrs.service.impl.util.RequestValidationUtil;
import org.wso2.carbon.device.mgt.jaxrs.util.DeviceMgtAPIUtils;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.multiplecredentials.UserDoesNotExistException;

import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.List;

public class GroupManagementServiceImpl implements GroupManagementService {

    private static final Log log = LogFactory.getLog(GroupManagementServiceImpl.class);

    private static final String DEFAULT_ADMIN_ROLE = "admin";
    private static final String[] DEFAULT_ADMIN_PERMISSIONS = {"/permission/device-mgt/admin/groups",
                                                               "/permission/device-mgt/user/groups"};
    private static final String EMPTY_RESULT = "EMPTY";

    @Override
    public Response getGroups(String name, String owner, int offset, int limit) {
        try {
            RequestValidationUtil.validatePaginationParameters(offset, limit);
            String currentUser = CarbonContext.getThreadLocalCarbonContext().getUsername();
            GroupPaginationRequest request = new GroupPaginationRequest(offset, limit);
            request.setGroupName(name);
            request.setOwner(owner);
            PaginationResult deviceGroupsResult = DeviceMgtAPIUtils.getGroupManagementProviderService()
                    .getGroups(currentUser, request);
            if (deviceGroupsResult.getData() != null && deviceGroupsResult.getRecordsTotal() > 0) {
                DeviceGroupList deviceGroupList = new DeviceGroupList();
                deviceGroupList.setList(deviceGroupsResult.getData());
                deviceGroupList.setCount(deviceGroupsResult.getRecordsTotal());
                return Response.status(Response.Status.OK).entity(deviceGroupList).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (GroupManagementException e) {
            String error = "Error occurred while getting the groups.";
            log.error(error, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
        }
    }

    @Override
    public Response getGroupCount() {
        try {
            String currentUser = CarbonContext.getThreadLocalCarbonContext().getUsername();
            int count = DeviceMgtAPIUtils.getGroupManagementProviderService().getGroupCount(currentUser);
            return Response.status(Response.Status.OK).entity(count).build();
        } catch (GroupManagementException e) {
            String msg = "Error occurred while retrieving group count.";
            log.error(msg, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(msg).build();
        }
    }

    @Override
    public Response createGroup(DeviceGroup group) {
        String owner = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
        if (group == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        group.setOwner(owner);
        group.setDateOfCreation(new Date().getTime());
        group.setDateOfLastUpdate(new Date().getTime());
        try {
            DeviceMgtAPIUtils.getGroupManagementProviderService().createGroup(group, DEFAULT_ADMIN_ROLE, DEFAULT_ADMIN_PERMISSIONS);
            return Response.status(Response.Status.CREATED).build();
        } catch (GroupManagementException e) {
            String msg = "Error occurred while adding new group.";
            log.error(msg, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(msg).build();
        } catch (GroupAlreadyExistException e) {
            String msg = "Group already exists with name '" + group.getName() + "'.";
            log.warn(msg);
            return Response.status(Response.Status.CONFLICT).entity(msg).build();
        }
    }

    @Override
    public Response getGroup(int groupId) {
        try {
            GroupManagementProviderService service = DeviceMgtAPIUtils.getGroupManagementProviderService();
            DeviceGroup deviceGroup = service.getGroup(groupId);
            if (deviceGroup != null) {
                return Response.status(Response.Status.OK).entity(deviceGroup).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (GroupManagementException e) {
            String error = "Error occurred while getting the group.";
            log.error(error, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
        }
    }

    @Override
    public Response updateGroup(int groupId, DeviceGroup deviceGroup) {
        if (deviceGroup == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        try {
            DeviceMgtAPIUtils.getGroupManagementProviderService().updateGroup(deviceGroup, groupId);
            return Response.status(Response.Status.OK).build();
        } catch (GroupManagementException e) {
            String msg = "Error occurred while adding new group.";
            log.error(msg, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(msg).build();
        } catch (GroupAlreadyExistException e) {
            String msg = "There is another group already exists with name '" + deviceGroup.getName() + "'.";
            log.warn(msg);
            return Response.status(Response.Status.CONFLICT).entity(msg).build();
        }
    }

    @Override
    public Response deleteGroup(int groupId) {
        try {
            if (DeviceMgtAPIUtils.getGroupManagementProviderService().deleteGroup(groupId)) {
                return Response.status(Response.Status.OK).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).entity("Group not found.").build();
            }
        } catch (GroupManagementException e) {
            String msg = "Error occurred while deleting the group.";
            log.error(msg, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(msg).build();
        }
    }

    @Override
    public Response manageGroupSharing(int groupId, DeviceGroupShare deviceGroupShare) {
        try {
            DeviceMgtAPIUtils.getGroupManagementProviderService()
                    .manageGroupSharing(groupId, deviceGroupShare.getUsername(), deviceGroupShare.getGroupRoles());
            return Response.status(Response.Status.OK).build();
        } catch (GroupManagementException e) {
            String msg = "Error occurred while managing group share.";
            log.error(msg, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(msg).build();
        } catch (RoleDoesNotExistException | UserDoesNotExistException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @Override
    public Response getUsersOfGroup(int groupId) {
        try {
            List<GroupUser> groupUsers = DeviceMgtAPIUtils.getGroupManagementProviderService().getUsers(groupId);
            if (groupUsers != null && groupUsers.size() > 0) {
                DeviceGroupUsersList deviceGroupUsersList = new DeviceGroupUsersList();
                deviceGroupUsersList.setList(groupUsers);
                deviceGroupUsersList.setCount(groupUsers.size());
                return Response.status(Response.Status.OK).entity(deviceGroupUsersList).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (GroupManagementException e) {
            String msg = "Error occurred while getting users of the group.";
            log.error(msg, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(msg).build();
        }
    }

    @Override
    public Response createGroupSharingRole(int groupId, String userName, RoleInfo roleInfo) {
        try {
            DeviceMgtAPIUtils.getGroupManagementProviderService()
                    .addGroupSharingRole(userName, groupId, roleInfo.getRoleName(), roleInfo.getPermissions());
            return Response.status(Response.Status.CREATED).build();
        } catch (GroupManagementException e) {
            String msg = "Error occurred while creating group sharing role.";
            log.error(msg, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(msg).build();
        }
    }

    @Override
    public Response getRolesOfGroup(int groupId, String userName) {
        try {
            List<String> groupRoles;
            if(userName != null) {
                UserStoreManager userStoreManager = DeviceMgtAPIUtils.getUserStoreManager();
                if (!userStoreManager.isExistingUser(userName)) {
                    // returning response with bad request state
                    return Response.status(Response.Status.CONFLICT).entity(
                            new ErrorResponse.ErrorResponseBuilder().setMessage("User by username: " +
                                    userName + " doesn't exists. Therefore, request made to get user " +
                                    "was refused.").build()).build();
                }
                groupRoles = DeviceMgtAPIUtils.getGroupManagementProviderService().getRoles(userName, groupId);
            } else {
                groupRoles = DeviceMgtAPIUtils.getGroupManagementProviderService().getRoles(groupId);
            }

            if(groupRoles != null && groupRoles.size() > 0) {
                RoleList deviceGroupRolesList = new RoleList();
                deviceGroupRolesList.setList(groupRoles);
                deviceGroupRolesList.setCount(groupRoles.size());
                return Response.status(Response.Status.OK).entity(deviceGroupRolesList).build();
            } else {
                return Response.status(Response.Status.OK).entity(EMPTY_RESULT).build();
            }
        } catch (GroupManagementException e) {
            String msg = "Error occurred while getting roles of the group.";
            log.error(msg, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(msg).build();
        } catch (UserStoreException e) {
            String msg = "Error while retrieving the user.";
            log.error(msg, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(msg).build();
        }
    }

    @Override
    public Response getDevicesOfGroup(int groupId, int offset, int limit) {
        try {
            GroupManagementProviderService service = DeviceMgtAPIUtils.getGroupManagementProviderService();
            List<Device> deviceList = service.getDevices(groupId, offset, limit);
            if (deviceList != null && deviceList.size() > 0) {
                DeviceList deviceListWrapper = new DeviceList();
                deviceListWrapper.setList(deviceList);
                deviceListWrapper.setCount(service.getDeviceCount(groupId));
                return Response.status(Response.Status.OK).entity(deviceListWrapper).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (GroupManagementException e) {
            String msg = "Error occurred while getting devices the group.";
            log.error(msg, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(msg).build();
        }
    }

    @Override
    public Response getDeviceCountOfGroup(int groupId) {
        try {
            int count = DeviceMgtAPIUtils.getGroupManagementProviderService().getDeviceCount(groupId);
            return Response.status(Response.Status.OK).entity(count).build();
        } catch (GroupManagementException e) {
            String msg = "Error occurred while getting device count of the group.";
            log.error(msg, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(msg).build();
        }
    }

    @Override
    public Response addDevicesToGroup(int groupId, List<DeviceIdentifier> deviceIdentifiers) {
        try {
            DeviceMgtAPIUtils.getGroupManagementProviderService().addDevices(groupId, deviceIdentifiers);
            return Response.status(Response.Status.OK).build();
        } catch (GroupManagementException e) {
            String msg = "Error occurred while adding devices to group.";
            log.error(msg, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(msg).build();
        } catch (DeviceNotFoundException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @Override public Response removeDevicesFromGroup(int groupId, List<DeviceIdentifier> deviceIdentifiers) {
        try {
            DeviceMgtAPIUtils.getGroupManagementProviderService().removeDevice(groupId, deviceIdentifiers);
            return Response.status(Response.Status.OK).build();
        } catch (GroupManagementException e) {
            String msg = "Error occurred while removing devices from group.";
            log.error(msg, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(msg).build();
        } catch (DeviceNotFoundException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

}