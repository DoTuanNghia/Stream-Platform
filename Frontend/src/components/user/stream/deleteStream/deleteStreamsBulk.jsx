import React, { useEffect, useRef, useState } from "react";
import "./deleteStreamsBulk.scss";

const DeleteStreamsBulk = ({ isOpen, onClose, onDelete, streams }) => {
    const [selectedIds, setSelectedIds] = useState(new Set());
    const [searchText, setSearchText] = useState("");
    const overlayMouseDownOnBackdropRef = useRef(false);

    // Reset selection and search when modal opens
    useEffect(() => {
        if (isOpen) {
            setSelectedIds(new Set());
            setSearchText("");
        }
    }, [isOpen]);

    // Filter streams based on search text (like SQL LIKE 'abc%')
    const filteredStreams = streams.filter((st) => {
        if (!searchText.trim()) return true;
        const query = searchText.toLowerCase();
        return (
            st.name?.toLowerCase().includes(query) ||
            st.keyStream?.toLowerCase().includes(query)
        );
    });

    if (!isOpen) return null;

    const handleOverlayMouseDown = (e) => {
        overlayMouseDownOnBackdropRef.current = e.target === e.currentTarget;
    };

    const handleOverlayMouseUp = (e) => {
        const mouseUpOnBackdrop = e.target === e.currentTarget;
        if (overlayMouseDownOnBackdropRef.current && mouseUpOnBackdrop) onClose();
        overlayMouseDownOnBackdropRef.current = false;
    };

    const handleToggle = (id) => {
        setSelectedIds((prev) => {
            const next = new Set(prev);
            if (next.has(id)) {
                next.delete(id);
            } else {
                next.add(id);
            }
            return next;
        });
    };

    const handleToggleAll = () => {
        // Toggle all FILTERED streams
        const filteredIds = filteredStreams.map((s) => s.id);
        const allFilteredSelected = filteredIds.every((id) => selectedIds.has(id));

        if (allFilteredSelected) {
            // Deselect all filtered
            setSelectedIds((prev) => {
                const next = new Set(prev);
                filteredIds.forEach((id) => next.delete(id));
                return next;
            });
        } else {
            // Select all filtered
            setSelectedIds((prev) => {
                const next = new Set(prev);
                filteredIds.forEach((id) => next.add(id));
                return next;
            });
        }
    };

    const handleSubmit = (e) => {
        e.preventDefault();

        if (selectedIds.size === 0) {
            alert("Vui lòng chọn ít nhất 1 luồng để xóa.");
            return;
        }

        onDelete(Array.from(selectedIds));
    };

    const filteredIds = filteredStreams.map((s) => s.id);
    const isAllFilteredSelected = filteredStreams.length > 0 && filteredIds.every((id) => selectedIds.has(id));

    return (
        <div className="modal-overlay" onMouseDown={handleOverlayMouseDown} onMouseUp={handleOverlayMouseUp}>
            <div className="modal modal--delete-bulk" onMouseDown={(e) => e.stopPropagation()} onMouseUp={(e) => e.stopPropagation()}>
                <div className="modal__header">
                    <span className="modal__title">Xóa Nhiều Luồng</span>
                    <button className="modal__close" onClick={onClose}>
                        ×
                    </button>
                </div>

                <form className="modal__body" onSubmit={handleSubmit}>
                    {/* Search input */}
                    <div className="delete-bulk__search">
                        <input
                            type="text"
                            placeholder="Tìm kiếm luồng (tên hoặc key)..."
                            value={searchText}
                            onChange={(e) => setSearchText(e.target.value)}
                            className="delete-bulk__search-input"
                        />
                        {searchText && (
                            <button
                                type="button"
                                className="delete-bulk__search-clear"
                                onClick={() => setSearchText("")}
                            >
                                ×
                            </button>
                        )}
                    </div>

                    {streams.length === 0 ? (
                        <p className="delete-bulk__empty">Không có luồng nào.</p>
                    ) : filteredStreams.length === 0 ? (
                        <p className="delete-bulk__empty">Không tìm thấy luồng nào phù hợp.</p>
                    ) : (
                        <>
                            <div className="delete-bulk__list">
                                {/* Header row với checkbox chọn tất cả */}
                                <div className="delete-bulk__row delete-bulk__row--header">
                                    <label className="delete-bulk__checkbox">
                                        <input
                                            type="checkbox"
                                            checked={isAllFilteredSelected}
                                            onChange={handleToggleAll}
                                        />
                                        <span className="checkmark"></span>
                                    </label>
                                    <span className="delete-bulk__name">Tên luồng</span>
                                    <span className="delete-bulk__key">Key Live</span>
                                </div>

                                {/* Stream rows */}
                                {filteredStreams.map((st) => (
                                    <div key={st.id} className={`delete-bulk__row ${selectedIds.has(st.id) ? "delete-bulk__row--selected" : ""}`}>
                                        <label className="delete-bulk__checkbox">
                                            <input
                                                type="checkbox"
                                                checked={selectedIds.has(st.id)}
                                                onChange={() => handleToggle(st.id)}
                                            />
                                            <span className="checkmark"></span>
                                        </label>
                                        <span className="delete-bulk__name">{st.name}</span>
                                        <span className="delete-bulk__key">{st.keyStream}</span>
                                    </div>
                                ))}
                            </div>

                            <div className="delete-bulk__hint">
                                Đã chọn <strong>{selectedIds.size}</strong> / {streams.length} luồng
                                {searchText && ` (hiển thị ${filteredStreams.length} kết quả)`}
                            </div>
                        </>
                    )}

                    <div className="modal__footer">
                        <button
                            type="submit"
                            className={`btn btn--danger modal__submit ${selectedIds.size === 0 ? "btn--disabled" : ""}`}
                            disabled={selectedIds.size === 0}
                        >
                            Xóa {selectedIds.size} luồng
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default DeleteStreamsBulk;
