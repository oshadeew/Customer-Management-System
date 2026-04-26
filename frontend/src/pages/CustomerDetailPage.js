import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getCustomerById } from '../services/customerService';

const CustomerDetailPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [customer, setCustomer] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchCustomer = async () => {
      try {
        const response = await getCustomerById(id);
        setCustomer(response.data);
      } catch (err) {
        setError(
          err.response?.data?.message || 'Failed to load customer details.'
        );
      } finally {
        setLoading(false);
      }
    };
    fetchCustomer();
  }, [id]);

  const formatDate = (dateStr) => {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleDateString('en-GB');
  };

  if (loading) {
    return (
      <div className="text-center py-5">
        <div className="spinner-border text-primary" role="status" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="alert alert-danger" role="alert">
        {error}
        <div className="mt-3">
          <button className="btn btn-secondary btn-sm" onClick={() => navigate('/')}>
            Back to List
          </button>
        </div>
      </div>
    );
  }

  if (!customer) return null;

  return (
    <div>
      {/* Header */}
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h1 className="page-title mb-0">Customer Details</h1>
        <div>
          <button
            id="btn-back"
            className="btn btn-outline-secondary me-2"
            onClick={() => navigate('/')}
          >
            ← Back
          </button>
          <button
            id="btn-edit-customer"
            className="btn btn-primary"
            onClick={() => navigate(`/customers/${id}/edit`)}
          >
            ✏️ Edit
          </button>
        </div>
      </div>

      {/* Basic Info */}
      <div className="card mb-4">
        <div className="card-header bg-primary text-white fw-semibold">
          Basic Information
        </div>
        <div className="card-body">
          <div className="row g-3">
            <div className="col-md-4">
              <div className="detail-label">Name</div>
              <div className="detail-value">{customer.name}</div>
            </div>
            <div className="col-md-4">
              <div className="detail-label">NIC Number</div>
              <div className="detail-value">
                <code>{customer.nicNumber}</code>
              </div>
            </div>
            <div className="col-md-4">
              <div className="detail-label">Date of Birth</div>
              <div className="detail-value">{formatDate(customer.dateOfBirth)}</div>
            </div>
            <div className="col-md-4">
              <div className="detail-label">Created At</div>
              <div className="detail-value">
                {customer.createdAt
                  ? new Date(customer.createdAt).toLocaleString('en-GB')
                  : '—'}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Phone Numbers */}
      <div className="card mb-4">
        <div className="card-header bg-secondary text-white fw-semibold">
          Phone Numbers
        </div>
        <div className="card-body">
          {customer.phones && customer.phones.length > 0 ? (
            <ul className="list-group list-group-flush">
              {customer.phones.map((p) => (
                <li key={p.id} className="list-group-item ps-0">
                  📞 {p.phoneNumber}
                </li>
              ))}
            </ul>
          ) : (
            <p className="text-muted mb-0">No phone numbers on record.</p>
          )}
        </div>
      </div>

      {/* Addresses */}
      <div className="card mb-4">
        <div className="card-header bg-secondary text-white fw-semibold">
          Addresses
        </div>
        <div className="card-body">
          {customer.addresses && customer.addresses.length > 0 ? (
            customer.addresses.map((a, idx) => (
              <div
                key={a.id}
                className={`address-block ${
                  idx < customer.addresses.length - 1 ? 'mb-3' : 'mb-0'
                }`}
              >
                <div className="row g-2">
                  <div className="col-md-6">
                    <div className="detail-label">Address Line 1</div>
                    <div className="detail-value">{a.addressLine1}</div>
                  </div>
                  {a.addressLine2 && (
                    <div className="col-md-6">
                      <div className="detail-label">Address Line 2</div>
                      <div className="detail-value">{a.addressLine2}</div>
                    </div>
                  )}
                  <div className="col-md-4">
                    <div className="detail-label">City</div>
                    <div className="detail-value">{a.cityName || '—'}</div>
                  </div>
                  <div className="col-md-4">
                    <div className="detail-label">Country</div>
                    <div className="detail-value">{a.countryName || '—'}</div>
                  </div>
                </div>
              </div>
            ))
          ) : (
            <p className="text-muted mb-0">No addresses on record.</p>
          )}
        </div>
      </div>

      {/* Family Members */}
      <div className="card mb-4">
        <div className="card-header bg-secondary text-white fw-semibold">
          Family Members
        </div>
        <div className="card-body">
          {customer.familyMembers && customer.familyMembers.length > 0 ? (
            <div className="table-responsive">
              <table className="table table-sm mb-0" id="family-table">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>NIC</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {customer.familyMembers.map((fm) => (
                    <tr key={fm.id}>
                      <td>{fm.name}</td>
                      <td>
                        <code>{fm.nicNumber}</code>
                      </td>
                      <td>
                        <button
                          id={`btn-view-family-${fm.id}`}
                          className="btn btn-sm btn-outline-primary"
                          onClick={() => navigate(`/customers/${fm.id}`)}
                        >
                          View
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <p className="text-muted mb-0">No family members linked.</p>
          )}
        </div>
      </div>
    </div>
  );
};

export default CustomerDetailPage;
