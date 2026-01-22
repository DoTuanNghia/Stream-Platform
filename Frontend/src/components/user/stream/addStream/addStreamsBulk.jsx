import React, { useEffect, useMemo, useRef, useState } from "react";
import "./addStreamsBulk.scss";

const splitLines = (text) =>
  (text || "")
    .split(/\r?\n/g)
    .map((s) => s.trim())
    .filter((s) => s.length > 0);

const AddStreamsBulk = ({ isOpen, onClose, onSave }) => {
  const [namesText, setNamesText] = useState("");
  const [keysText, setKeysText] = useState("");
  const overlayMouseDownOnBackdropRef = useRef(false);

  useEffect(() => {
    if (!isOpen) return;
    setNamesText("");
    setKeysText("");
  }, [isOpen]);

  const { names, keys } = useMemo(() => {
    return {
      names: splitLines(namesText),
      keys: splitLines(keysText),
    };
  }, [namesText, keysText]);

  if (!isOpen) return null;

  const handleOverlayMouseDown = (e) => {
    overlayMouseDownOnBackdropRef.current = e.target === e.currentTarget;
  };

  const handleOverlayMouseUp = (e) => {
    const mouseUpOnBackdrop = e.target === e.currentTarget;
    if (overlayMouseDownOnBackdropRef.current && mouseUpOnBackdrop) onClose();
    overlayMouseDownOnBackdropRef.current = false;
  };

  const handleSubmit = (e) => {
    e.preventDefault();

    if (names.length === 0 || keys.length === 0) {
      alert("Vui lòng nhập danh sách Tên luồng và Key live.");
      return;
    }
    if (names.length !== keys.length) {
      alert(`Số dòng không khớp: Tên luồng = ${names.length}, Key live = ${keys.length}.`);
      return;
    }

    const items = names.map((name, i) => ({
      name,
      keyStream: keys[i],
    }));

    onSave(items);
  };

  return (
    <div className="modal-overlay" onMouseDown={handleOverlayMouseDown} onMouseUp={handleOverlayMouseUp}>
      <div className="modal" onMouseDown={(e) => e.stopPropagation()} onMouseUp={(e) => e.stopPropagation()}>
        <div className="modal__header">
          <span className="modal__title">Thêm nhiều Stream</span>
          <button className="modal__close" onClick={onClose}>
            ×
          </button>
        </div>

        <form className="modal__body" onSubmit={handleSubmit}>
          <div className="bulk-grid">
            <div className="modal__field">
              <label>Tên luồng (mỗi dòng 1 tên)</label>
              <textarea
                rows={10}
                value={namesText}
                onChange={(e) => setNamesText(e.target.value)}
                placeholder={"demo 1\ndemo 2\ndemo 3"}
              />
            </div>

            <div className="modal__field">
              <label>Key live (mỗi dòng 1 key)</label>
              <textarea
                rows={10}
                value={keysText}
                onChange={(e) => setKeysText(e.target.value)}
                placeholder={"abc1\nabc2\nabc3"}
              />
            </div>
          </div>

          <div className="bulk-hint">
            Sẽ tạo <strong>{Math.min(names.length, keys.length)}</strong> luồng (match theo thứ tự dòng).
          </div>

          <div className="modal__footer">
            <button type="submit" className="btn btn--primary modal__submit">
              Tạo {Math.min(names.length, keys.length)} luồng
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default AddStreamsBulk;

