// src/utils/googleDriveDownloader.js
// Utility để tải video từ Google Drive về VPS

const DOWNLOAD_API_BASE = '/dlapi';
const AUTH_TOKEN = 's3cureT0k3n#2024!';

/**
 * Kiểm tra xem một URL có phải là link Google Drive không
 * @param {string} url 
 * @returns {boolean}
 */
export const isGoogleDriveUrl = (url) => {
    if (!url || typeof url !== 'string') return false;
    const trimmed = url.trim().toLowerCase();
    return (
        trimmed.includes('drive.google.com') ||
        trimmed.includes('docs.google.com') ||
        trimmed.startsWith('https://drive.google.com') ||
        trimmed.startsWith('http://drive.google.com')
    );
};

/**
 * Lấy danh sách files đã tải trên VPS để xác định số thứ tự tiếp theo
 * @returns {Promise<Array>}
 */
export const fetchDownloadedFiles = async () => {
    try {
        const response = await fetch(`${DOWNLOAD_API_BASE}/list_files`, {
            method: 'GET',
            headers: {
                'Authorization': AUTH_TOKEN
            }
        });

        const result = await response.json().catch(() => ({}));

        if (response.ok) {
            return result.files || [];
        }
        return [];
    } catch (error) {
        console.error('Error fetching downloaded files:', error);
        return [];
    }
};

/**
 * Tự động tạo tên file theo format ddmmyy_xx.mp4
 * dd = ngày, mm = tháng, yy = 2 số cuối năm, xx = số thứ tự từ 01
 * Dựa trên danh sách files đã có trên VPS
 * @returns {Promise<string>}
 */
export const generateAutoFileName = async () => {
    const now = new Date();
    const dd = String(now.getDate()).padStart(2, '0');
    const mm = String(now.getMonth() + 1).padStart(2, '0');
    const yy = String(now.getFullYear()).slice(-2);
    const datePrefix = `${dd}${mm}${yy}`;

    const files = await fetchDownloadedFiles();

    // Lấy tất cả số thứ tự từ file có dạng ddmmyy_XX.mp4 (cùng ngày)
    const pattern = new RegExp(`^${datePrefix}_(\\d+)\\.mp4$`, 'i');
    const usedNumbers = files
        .map(f => f.fileName)
        .map(name => {
            const match = name.match(pattern);
            return match ? parseInt(match[1], 10) : NaN;
        })
        .filter(n => !isNaN(n));

    // Tìm số nhỏ nhất chưa được dùng
    let nextNumber = 1;
    while (usedNumbers.includes(nextNumber)) {
        nextNumber++;
    }

    const xx = String(nextNumber).padStart(2, '0');
    return `${datePrefix}_${xx}.mp4`;
};

/**
 * Kiểm tra file đã tồn tại trên VPS chưa
 * @param {string} fileName 
 * @returns {Promise<boolean>}
 */
export const checkFileExists = async (fileName) => {
    const files = await fetchDownloadedFiles();
    return files.some(f => f.fileName === fileName);
};

/**
 * Gọi API download video từ Google Drive về VPS
 * @param {string} driveUrl - URL Google Drive
 * @param {string} fileName - Tên file output (có đuôi .mp4)
 * @returns {Promise<{success: boolean, message: string, fileName: string}>}
 */
export const downloadFromGoogleDrive = async (driveUrl, fileName) => {
    try {
        // Đảm bảo có đuôi .mp4
        const finalFileName = fileName.toLowerCase().endsWith('.mp4')
            ? fileName
            : `${fileName}.mp4`;

        // Kiểm tra file đã tồn tại chưa
        const exists = await checkFileExists(finalFileName);
        if (exists) {
            return {
                success: false,
                message: `File "${finalFileName}" already exists on server.`,
                fileName: finalFileName
            };
        }

        // Gọi API download
        const payload = {
            file_type: 'drive',
            file_id_or_url: driveUrl.trim(),
            file_name: finalFileName
        };

        const response = await fetch(`${DOWNLOAD_API_BASE}/download`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        const result = await response.json().catch(() => ({}));

        if (response.ok) {
            return {
                success: true,
                message: result.message || 'Download started successfully.',
                fileName: finalFileName
            };
        } else {
            return {
                success: false,
                message: result.error || `HTTP ${response.status}`,
                fileName: finalFileName
            };
        }
    } catch (error) {
        return {
            success: false,
            message: error.message || 'Unknown error occurred.',
            fileName: fileName
        };
    }
};

/**
 * Hàm chính: Xử lý download từ Google Drive và trả về tên file mới
 * Dùng trong addStream khi phát hiện URL Google Drive
 * @param {string} driveUrl 
 * @returns {Promise<{success: boolean, fileName: string, message: string}>}
 */
export const processGoogleDriveDownload = async (driveUrl) => {
    // 1. Tự động tạo tên file
    const autoFileName = await generateAutoFileName();

    // 2. Bắt đầu download
    const result = await downloadFromGoogleDrive(driveUrl, autoFileName);

    return result;
};

export default {
    isGoogleDriveUrl,
    fetchDownloadedFiles,
    generateAutoFileName,
    checkFileExists,
    downloadFromGoogleDrive,
    processGoogleDriveDownload
};
