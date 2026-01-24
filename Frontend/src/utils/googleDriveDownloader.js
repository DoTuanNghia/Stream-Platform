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
 * Tự động tạo tên file theo số thứ tự (1.mp4, 2.mp4, ...)
 * Dựa trên danh sách files đã có trên VPS
 * @returns {Promise<string>}
 */
export const generateAutoFileName = async () => {
    const files = await fetchDownloadedFiles();

    // Lấy tất cả các số từ tên file hiện có (dạng X.mp4)
    const usedNumbers = files
        .map(f => f.fileName)
        .filter(name => /^\d+\.mp4$/i.test(name))
        .map(name => parseInt(name.replace('.mp4', ''), 10))
        .filter(n => !isNaN(n));

    // Tìm số nhỏ nhất chưa được dùng
    let nextNumber = 1;
    while (usedNumbers.includes(nextNumber)) {
        nextNumber++;
    }

    return `${nextNumber}.mp4`;
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
