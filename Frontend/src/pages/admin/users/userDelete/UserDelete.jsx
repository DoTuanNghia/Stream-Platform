import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { memberApi } from "@/api/admin/member.api";
import "./userDelete.scss";

export default function UserDelete() {
  const navigate = useNavigate();
  const { id } = useParams();

  const [loading, setLoading] = useState(true);
  const [deleting, setDeleting] = useState(false);
  const [member, setMember] = useState(null);

  const fetchMember = async () => {
    setLoading(true);
    try {
      const res = await memberApi.getById(id);
      setMember(res?.data ?? res);
    } catch (err) {
      console.error(err);
      window.alert(err?.response?.data || "Không tải được dữ liệu người dùng.");
      navigate("/admin/users", { replace: true });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMember();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const onDelete = async () => {
    const ok = window.confirm("Bạn chắc chắn muốn xoá người dùng này?");
    if (!ok) return;

    setDeleting(true);
    try {
      await memberApi.remove(id);
      window.alert("Xoá người dùng thành công");
      navigate("/admin/users", { replace: true });
    } catch (err) {
      console.error(err);
      window.alert(err?.response?.data || "Xoá thất bại.");
    } finally {
      setDeleting(false);
    }
  };

  if (loading) {
    return <div className="admin-user-delete__loading">Đang tải...</div>;
  }

  return (
    <div className="admin-user-delete">
      <div className="admin-user-delete__header">
        <h2>Xoá người dùng</h2>
      </div>

      <div className="admin-user-delete__card">
        <p className="admin-user-delete__warn">
          Hành động này không thể hoàn tác.
        </p>

        <div className="admin-user-delete__info">
          <div className="row">
            <span className="k">Tên</span>
            <span className="v">{member?.name ?? member?.fullName ?? "n/a"}</span>
          </div>
          <div className="row">
            <span className="k">Username</span>
            <span className="v">{member?.username ?? "n/a"}</span>
          </div>
          <div className="row">
            <span className="k">Role</span>
            <span className="v">{String(member?.role ?? "USER")}</span>
          </div>
        </div>

        <div className="actions">
          <button
            type="button"
            className="btn"
            onClick={() => navigate("/admin/users")}
            disabled={deleting}
          >
            Huỷ
          </button>

          <button
            type="button"
            className="btn btn--danger"
            onClick={onDelete}
            disabled={deleting}
          >
            {deleting ? "Đang xoá..." : "Xoá"}
          </button>
        </div>
      </div>
    </div>
  );
}
