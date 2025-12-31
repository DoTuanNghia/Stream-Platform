import axiosClient from "../axiosClient";

export const channelApi = {
  countByUserId: (userId) => axiosClient.get(`/channels/user/${userId}/count`),
};
