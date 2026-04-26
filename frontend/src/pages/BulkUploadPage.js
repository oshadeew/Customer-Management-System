import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { bulkUploadCustomers } from '../services/customerService';

const BulkUploadPage = () => {
  const navigate = useNavigate();
  const [file, setFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');

  const handleFileChange = (e) => {
    const selected = e.target.files[0];
    setFile(selected);
    setResult(null);
    setError('');
  };

  const handleUpload = async () => {
    if (!file) {
      setError('Please select an .xlsx file to upload.');
      return;
    }
    if (!file.name.endsWith('.xlsx')) {
      setError('Only .xlsx files are accepted.');
      return;
    }

    setUploading(true);
    setError('');
    setResult(null);

    try {
      const response = await bulkUploadCustomers(file);
      setResult(response.data);
    } catch (err) {
      const msg =
        err.response?.data?.message ||
        err.response?.data?.errors?.[0] ||
        'Upload failed. Please try again.';
      setError(msg);
    } finally {
      setUploading(false);
    }
  };

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h1 className="page-title mb-0">Bulk Upload Customers</h1>
        <button
          id="btn-back"
          className="btn btn-outline-secondary"
          onClick={() => navigate('/')}
        >
          ← Back
        </button>
      </div>

      {/* Instructions */}
      <div className="card mb-4">
        <div className="card-header bg-info text-white fw-semibold">
          📄 Excel File Format
        </div>
        <div className="card-body">
          <p className="mb-2">
            Upload an <strong>.xlsx</strong> file with the following columns (first row = header, skipped automatically):
          </p>
          <div className="table-responsive">
            <table className="table table-bordered table-sm">
              <thead className="table-light">
                <tr>
                  <th>Column A</th>
                  <th>Column B</th>
                  <th>Column C</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>
                    <strong>Name</strong>
                    <br />
                    <small className="text-muted">Full customer name</small>
                  </td>
                  <td>
                    <strong>Date of Birth</strong>
                    <br />
                    <small className="text-muted">Format: yyyy-MM-dd (e.g., 1990-05-15)</small>
                  </td>
                  <td>
                    <strong>NIC Number</strong>
                    <br />
                    <small className="text-muted">Unique identifier. If NIC exists → update; else → insert</small>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div className="alert alert-info mb-0 py-2">
            <small>
              <strong>Tip:</strong> The system automatically upserts — if the NIC already exists in the
              database, the customer's name and date of birth will be updated. New NICs will be inserted
              as new customers. Processed in batches of 500 rows.
            </small>
          </div>
        </div>
      </div>

      {/* Upload box */}
      <div className="card mb-4">
        <div className="card-body">
          <div className="upload-box mb-3">
            <div className="mb-3">
              <span style={{ fontSize: '2.5rem' }}>📂</span>
            </div>
            <label htmlFor="file-input" className="btn btn-outline-primary mb-2">
              Choose .xlsx File
            </label>
            <input
              id="file-input"
              type="file"
              accept=".xlsx"
              onChange={handleFileChange}
              className="d-none"
            />
            {file && (
              <p className="mb-0 mt-2 text-success fw-semibold">
                ✅ Selected: {file.name} ({(file.size / 1024).toFixed(1)} KB)
              </p>
            )}
          </div>

          {error && (
            <div className="alert alert-danger error-alert" role="alert">
              {error}
            </div>
          )}

          <button
            id="btn-upload"
            className="btn btn-primary btn-lg w-100"
            onClick={handleUpload}
            disabled={uploading || !file}
          >
            {uploading ? (
              <>
                <span className="spinner-border spinner-border-sm me-2" />
                Processing...
              </>
            ) : (
              '⬆️ Upload & Process'
            )}
          </button>
        </div>
      </div>

      {/* Result summary */}
      {result && (
        <div className="card">
          <div className="card-header bg-success text-white fw-semibold">
            ✅ Upload Result Summary
          </div>
          <div className="card-body">
            <div className="row g-3 mb-4">
              <div className="col-6 col-md-3">
                <div className="text-center p-3 bg-light rounded">
                  <div className="display-6 fw-bold text-primary">{result.totalRows}</div>
                  <small className="text-muted">Total Rows</small>
                </div>
              </div>
              <div className="col-6 col-md-3">
                <div className="text-center p-3 bg-light rounded">
                  <div className="display-6 fw-bold text-success">{result.successCount}</div>
                  <small className="text-muted">Inserted</small>
                </div>
              </div>
              <div className="col-6 col-md-3">
                <div className="text-center p-3 bg-light rounded">
                  <div className="display-6 fw-bold text-info">{result.updatedCount}</div>
                  <small className="text-muted">Updated</small>
                </div>
              </div>
              <div className="col-6 col-md-3">
                <div className="text-center p-3 bg-light rounded">
                  <div className="display-6 fw-bold text-danger">{result.failedCount}</div>
                  <small className="text-muted">Failed</small>
                </div>
              </div>
            </div>

            {result.errors && result.errors.length > 0 && (
              <div>
                <h6 className="fw-semibold text-danger">Errors ({result.errors.length}):</h6>
                <div
                  className="bg-light p-3 rounded"
                  style={{ maxHeight: '200px', overflowY: 'auto' }}
                  id="upload-errors"
                >
                  {result.errors.map((err, idx) => (
                    <div key={idx} className="text-danger small mb-1">
                      ❌ {err}
                    </div>
                  ))}
                </div>
              </div>
            )}

            <div className="mt-3">
              <button
                id="btn-view-customers"
                className="btn btn-outline-primary"
                onClick={() => navigate('/')}
              >
                View All Customers →
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default BulkUploadPage;
