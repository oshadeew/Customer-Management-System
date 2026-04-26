import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { getCustomers } from '../services/customerService';

const CustomerListPage = () => {
  const [customers, setCustomers] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const PAGE_SIZE = 10;

  const fetchCustomers = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await getCustomers(page, PAGE_SIZE);
      setCustomers(response.data.content || []);
      setTotalPages(response.data.totalPages || 0);
      setTotalElements(response.data.totalElements || 0);
    } catch (err) {
      setError('Failed to load customers. Please ensure the backend is running.');
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => {
    fetchCustomers();
  }, [fetchCustomers]);

  const formatDate = (dateStr) => {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleDateString('en-GB');
  };

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h1 className="page-title mb-0">Customers</h1>
        <button
          id="btn-new-customer"
          className="btn btn-primary"
          onClick={() => navigate('/customers/new')}
        >
          + New Customer
        </button>
      </div>

      {error && (
        <div className="alert alert-danger error-alert" role="alert">
          {error}
        </div>
      )}

      <div className="card">
        <div className="card-body p-0">
          {loading ? (
            <div className="text-center py-5">
              <div className="spinner-border text-primary" role="status" />
              <p className="mt-2 text-muted">Loading customers...</p>
            </div>
          ) : customers.length === 0 ? (
            <div className="text-center py-5 text-muted">
              <p className="mb-0">No customers found.</p>
              <small>Click "+ New Customer" to add the first one.</small>
            </div>
          ) : (
            <div className="table-responsive">
              <table className="table table-hover mb-0" id="customers-table">
                <thead>
                  <tr>
                    <th>#</th>
                    <th>Name</th>
                    <th>NIC Number</th>
                    <th>Date of Birth</th>
                    <th className="text-center">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {customers.map((c, idx) => (
                    <tr key={c.id}>
                      <td className="text-muted">{page * PAGE_SIZE + idx + 1}</td>
                      <td className="fw-semibold">{c.name}</td>
                      <td>
                        <code>{c.nicNumber}</code>
                      </td>
                      <td>{formatDate(c.dateOfBirth)}</td>
                      <td className="text-center">
                        <button
                          id={`btn-view-${c.id}`}
                          className="btn btn-sm btn-outline-primary me-2"
                          onClick={() => navigate(`/customers/${c.id}`)}
                        >
                          View
                        </button>
                        <button
                          id={`btn-edit-${c.id}`}
                          className="btn btn-sm btn-outline-secondary"
                          onClick={() => navigate(`/customers/${c.id}/edit`)}
                        >
                          Edit
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="card-footer d-flex justify-content-between align-items-center">
            <small className="text-muted">
              Showing {page * PAGE_SIZE + 1}–{Math.min((page + 1) * PAGE_SIZE, totalElements)} of{' '}
              {totalElements} customers
            </small>
            <nav aria-label="Customer pagination">
              <ul className="pagination pagination-sm mb-0">
                <li className={`page-item ${page === 0 ? 'disabled' : ''}`}>
                  <button
                    className="page-link"
                    id="btn-prev-page"
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    disabled={page === 0}
                  >
                    ← Prev
                  </button>
                </li>
                {[...Array(totalPages)].map((_, i) => (
                  <li key={i} className={`page-item ${i === page ? 'active' : ''}`}>
                    <button
                      className="page-link"
                      id={`btn-page-${i}`}
                      onClick={() => setPage(i)}
                    >
                      {i + 1}
                    </button>
                  </li>
                ))}
                <li className={`page-item ${page === totalPages - 1 ? 'disabled' : ''}`}>
                  <button
                    className="page-link"
                    id="btn-next-page"
                    onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                    disabled={page === totalPages - 1}
                  >
                    Next →
                  </button>
                </li>
              </ul>
            </nav>
          </div>
        )}
      </div>
    </div>
  );
};

export default CustomerListPage;
