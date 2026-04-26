import React, { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import DatePicker from 'react-datepicker';
import {
  getCustomerById,
  createCustomer,
  updateCustomer,
  getCountries,
  getCitiesByCountry,
  searchCustomers,
} from '../services/customerService';

//  Empty helpers

const emptyPhone = () => ({ phoneNumber: '' });

const emptyAddress = () => ({
  addressLine1: '',
  addressLine2: '',
  countryId: '',
  cityId: '',
  cities: [],
});

//  Component

const CustomerFormPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEditMode = Boolean(id);

  //  Form state
  const [form, setForm] = useState({
    name: '',
    dateOfBirth: null,
    nicNumber: '',
  });
  const [phones, setPhones] = useState([emptyPhone()]);
  const [addresses, setAddresses] = useState([emptyAddress()]);
  const [familyMemberIds, setFamilyMemberIds] = useState([]);

  //  Lookup data
  const [countries, setCountries] = useState([]);
  const [familySearchQuery, setFamilySearchQuery] = useState('');
  const [familySearchResults, setFamilySearchResults] = useState([]);
  const [selectedFamilyMembers, setSelectedFamilyMembers] = useState([]); // [{id, name, nicNumber}]

  //  UI state
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState('');
  const [loading, setLoading] = useState(false);
  const [pageLoading, setPageLoading] = useState(isEditMode);

  //  Load countries once
  useEffect(() => {
    getCountries()
      .then((r) => setCountries(r.data || []))
      .catch(() => setApiError('Failed to load countries.'));
  }, []);

  //  Load existing customer for edit mode
  useEffect(() => {
    if (!isEditMode) return;
    const fetchCustomer = async () => {
      try {
        const response = await getCustomerById(id);
        const c = response.data;

        setForm({
          name: c.name || '',
          dateOfBirth: c.dateOfBirth ? new Date(c.dateOfBirth) : null,
          nicNumber: c.nicNumber || '',
        });

        setPhones(
          c.phones && c.phones.length > 0
            ? c.phones.map((p) => ({ phoneNumber: p.phoneNumber }))
            : [emptyPhone()]
        );

        // Addresses: need to load cities for each address's country
        if (c.addresses && c.addresses.length > 0) {
          const loadedAddresses = await Promise.all(
            c.addresses.map(async (a) => {
              let cities = [];
              if (a.countryId) {
                try {
                  const cr = await getCitiesByCountry(a.countryId);
                  cities = cr.data || [];
                } catch (_) {}
              }
              return {
                addressLine1: a.addressLine1 || '',
                addressLine2: a.addressLine2 || '',
                countryId: a.countryId ? String(a.countryId) : '',
                cityId: a.cityId ? String(a.cityId) : '',
                cities,
              };
            })
          );
          setAddresses(loadedAddresses);
        }

        if (c.familyMembers && c.familyMembers.length > 0) {
          setSelectedFamilyMembers(c.familyMembers);
          setFamilyMemberIds(c.familyMembers.map((fm) => fm.id));
        }
      } catch (err) {
        setApiError('Failed to load customer data.');
      } finally {
        setPageLoading(false);
      }
    };
    fetchCustomer();
  }, [id, isEditMode]);

  //  Validation

  const validate = () => {
    const newErrors = {};
    if (!form.name.trim()) newErrors.name = 'Name is required.';
    if (!form.dateOfBirth) newErrors.dateOfBirth = 'Date of birth is required.';
    if (!form.nicNumber.trim()) newErrors.nicNumber = 'NIC number is required.';

    phones.forEach((p, i) => {
      if (!p.phoneNumber.trim()) {
        newErrors[`phone_${i}`] = 'Phone number cannot be blank.';
      }
    });

    addresses.forEach((a, i) => {
      if (!a.addressLine1.trim()) {
        newErrors[`addr_line1_${i}`] = 'Address Line 1 is required.';
      }
      if (!a.cityId) {
        newErrors[`addr_city_${i}`] = 'City is required.';
      }
    });

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  //  Phone handlers

  const addPhone = () => setPhones([...phones, emptyPhone()]);

  const removePhone = (idx) => setPhones(phones.filter((_, i) => i !== idx));

  const updatePhone = (idx, value) => {
    const updated = [...phones];
    updated[idx] = { phoneNumber: value };
    setPhones(updated);
  };

  //  Address handlers

  const addAddress = () => setAddresses([...addresses, emptyAddress()]);

  const removeAddress = (idx) => setAddresses(addresses.filter((_, i) => i !== idx));

  const updateAddressField = (idx, field, value) => {
    const updated = [...addresses];
    updated[idx] = { ...updated[idx], [field]: value };
    setAddresses(updated);
  };

  const handleCountryChange = async (idx, countryId) => {
    const updated = [...addresses];
    updated[idx] = { ...updated[idx], countryId, cityId: '', cities: [] };
    setAddresses(updated);

    if (countryId) {
      try {
        const response = await getCitiesByCountry(countryId);
        const newUpdated = [...addresses];
        newUpdated[idx] = {
          ...newUpdated[idx],
          countryId,
          cityId: '',
          cities: response.data || [],
        };
        setAddresses(newUpdated);
      } catch (_) {}
    }
  };

  //  Family member handlers

  const handleFamilySearch = useCallback(async (query) => {
    setFamilySearchQuery(query);
    if (query.length < 2) {
      setFamilySearchResults([]);
      return;
    }
    try {
      const response = await searchCustomers(query);
      // Exclude current customer and already selected members
      const filtered = (response.data || []).filter(
        (c) =>
          c.id !== Number(id) &&
          !familyMemberIds.includes(c.id)
      );
      setFamilySearchResults(filtered);
    } catch (_) {}
  }, [id, familyMemberIds]);

  const selectFamilyMember = (member) => {
    if (!familyMemberIds.includes(member.id)) {
      setFamilyMemberIds([...familyMemberIds, member.id]);
      setSelectedFamilyMembers([...selectedFamilyMembers, member]);
    }
    setFamilySearchQuery('');
    setFamilySearchResults([]);
  };

  const removeFamilyMember = (memberId) => {
    setFamilyMemberIds(familyMemberIds.filter((fid) => fid !== memberId));
    setSelectedFamilyMembers(selectedFamilyMembers.filter((fm) => fm.id !== memberId));
  };

  //  Submit

  const handleSubmit = async (e) => {
    e.preventDefault();
    setApiError('');
    if (!validate()) return;

    setLoading(true);

    const payload = {
      name: form.name.trim(),
      dateOfBirth: form.dateOfBirth
        ? form.dateOfBirth.toISOString().split('T')[0]
        : null,
      nicNumber: form.nicNumber.trim(),
      phones: phones
        .filter((p) => p.phoneNumber.trim())
        .map((p) => ({ phoneNumber: p.phoneNumber.trim() })),
      addresses: addresses
        .filter((a) => a.addressLine1.trim() && a.cityId)
        .map((a) => ({
          addressLine1: a.addressLine1.trim(),
          addressLine2: a.addressLine2.trim() || null,
          cityId: Number(a.cityId),
        })),
      familyMemberIds,
    };

    try {
      if (isEditMode) {
        await updateCustomer(id, payload);
        navigate(`/customers/${id}`);
      } else {
        const response = await createCustomer(payload);
        navigate(`/customers/${response.data.id}`);
      }
    } catch (err) {
      const data = err.response?.data;
      if (data?.details) {
        // Validation field errors from backend
        const fieldErrors = {};
        Object.entries(data.details).forEach(([key, val]) => {
          fieldErrors[key] = val;
        });
        setErrors(fieldErrors);
      }
      setApiError(data?.message || 'An error occurred. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  //  Render

  if (pageLoading) {
    return (
      <div className="text-center py-5">
        <div className="spinner-border text-primary" role="status" />
      </div>
    );
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h1 className="page-title mb-0">
          {isEditMode ? '✏️ Edit Customer' : '+ New Customer'}
        </h1>
        <button
          id="btn-back"
          className="btn btn-outline-secondary"
          onClick={() => navigate(isEditMode ? `/customers/${id}` : '/')}
        >
          ← Cancel
        </button>
      </div>

      {apiError && (
        <div className="alert alert-danger error-alert" role="alert">
          {apiError}
        </div>
      )}

      <form onSubmit={handleSubmit} noValidate id="customer-form">
        {/*  Basic Info  */}
        <div className="card mb-4">
          <div className="card-header bg-primary text-white fw-semibold">
            Basic Information
          </div>
          <div className="card-body">
            <div className="row g-3">
              {/* Name */}
              <div className="col-md-6">
                <label htmlFor="input-name" className="form-label fw-semibold">
                  Name <span className="text-danger">*</span>
                </label>
                <input
                  id="input-name"
                  type="text"
                  className={`form-control ${errors.name ? 'is-invalid' : ''}`}
                  placeholder="Full name"
                  value={form.name}
                  onChange={(e) => setForm({ ...form, name: e.target.value })}
                />
                {errors.name && (
                  <div className="invalid-feedback">{errors.name}</div>
                )}
              </div>

              {/* NIC */}
              <div className="col-md-6">
                <label htmlFor="input-nic" className="form-label fw-semibold">
                  NIC Number <span className="text-danger">*</span>
                </label>
                <input
                  id="input-nic"
                  type="text"
                  className={`form-control ${errors.nicNumber ? 'is-invalid' : ''}`}
                  placeholder="e.g. 901234567V"
                  value={form.nicNumber}
                  onChange={(e) => setForm({ ...form, nicNumber: e.target.value })}
                />
                {errors.nicNumber && (
                  <div className="invalid-feedback">{errors.nicNumber}</div>
                )}
              </div>

              {/* Date of Birth */}
              <div className="col-md-6">
                <label htmlFor="input-dob" className="form-label fw-semibold">
                  Date of Birth <span className="text-danger">*</span>
                </label>
                <DatePicker
                  id="input-dob"
                  selected={form.dateOfBirth}
                  onChange={(date) => setForm({ ...form, dateOfBirth: date })}
                  dateFormat="yyyy-MM-dd"
                  showYearDropdown
                  scrollableYearDropdown
                  yearDropdownItemNumber={80}
                  placeholderText="Select date of birth"
                  maxDate={new Date()}
                  className={errors.dateOfBirth ? 'is-invalid' : ''}
                />
                {errors.dateOfBirth && (
                  <div className="text-danger small mt-1">{errors.dateOfBirth}</div>
                )}
              </div>
            </div>
          </div>
        </div>

        {/*  Phone Numbers  */}
        <div className="card mb-4">
          <div className="card-header bg-secondary text-white d-flex justify-content-between align-items-center">
            <span className="fw-semibold">Phone Numbers</span>
            <button
              type="button"
              id="btn-add-phone"
              className="btn btn-light btn-sm"
              onClick={addPhone}
            >
              + Add Phone
            </button>
          </div>
          <div className="card-body">
            {phones.map((phone, idx) => (
              <div key={idx} className="phone-row">
                <input
                  id={`input-phone-${idx}`}
                  type="text"
                  className={`form-control ${errors[`phone_${idx}`] ? 'is-invalid' : ''}`}
                  placeholder="+94771234567"
                  value={phone.phoneNumber}
                  onChange={(e) => updatePhone(idx, e.target.value)}
                />
                {errors[`phone_${idx}`] && (
                  <div className="invalid-feedback d-block">{errors[`phone_${idx}`]}</div>
                )}
                {phones.length > 1 && (
                  <button
                    type="button"
                    id={`btn-remove-phone-${idx}`}
                    className="btn btn-outline-danger btn-sm"
                    onClick={() => removePhone(idx)}
                  >
                    ✕
                  </button>
                )}
              </div>
            ))}
          </div>
        </div>

        {/*  Addresses  */}
        <div className="card mb-4">
          <div className="card-header bg-secondary text-white d-flex justify-content-between align-items-center">
            <span className="fw-semibold">Addresses</span>
            <button
              type="button"
              id="btn-add-address"
              className="btn btn-light btn-sm"
              onClick={addAddress}
            >
              + Add Address
            </button>
          </div>
          <div className="card-body">
            {addresses.map((addr, idx) => (
              <div key={idx} className="address-block position-relative">
                {addresses.length > 1 && (
                  <button
                    type="button"
                    id={`btn-remove-addr-${idx}`}
                    className="btn btn-outline-danger btn-sm position-absolute top-0 end-0 m-2"
                    onClick={() => removeAddress(idx)}
                  >
                    ✕ Remove
                  </button>
                )}
                <div className="row g-3">
                  <div className="col-md-6">
                    <label className="form-label fw-semibold">
                      Address Line 1 <span className="text-danger">*</span>
                    </label>
                    <input
                      id={`input-addr-line1-${idx}`}
                      type="text"
                      className={`form-control ${errors[`addr_line1_${idx}`] ? 'is-invalid' : ''}`}
                      placeholder="Street address"
                      value={addr.addressLine1}
                      onChange={(e) =>
                        updateAddressField(idx, 'addressLine1', e.target.value)
                      }
                    />
                    {errors[`addr_line1_${idx}`] && (
                      <div className="invalid-feedback">{errors[`addr_line1_${idx}`]}</div>
                    )}
                  </div>
                  <div className="col-md-6">
                    <label className="form-label fw-semibold">Address Line 2</label>
                    <input
                      id={`input-addr-line2-${idx}`}
                      type="text"
                      className="form-control"
                      placeholder="Apartment, suite, etc. (optional)"
                      value={addr.addressLine2}
                      onChange={(e) =>
                        updateAddressField(idx, 'addressLine2', e.target.value)
                      }
                    />
                  </div>
                  <div className="col-md-6">
                    <label className="form-label fw-semibold">Country</label>
                    <select
                      id={`input-addr-country-${idx}`}
                      className="form-select"
                      value={addr.countryId}
                      onChange={(e) => handleCountryChange(idx, e.target.value)}
                    >
                      <option value="">— Select Country —</option>
                      {countries.map((c) => (
                        <option key={c.id} value={c.id}>
                          {c.name}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="col-md-6">
                    <label className="form-label fw-semibold">
                      City <span className="text-danger">*</span>
                    </label>
                    <select
                      id={`input-addr-city-${idx}`}
                      className={`form-select ${errors[`addr_city_${idx}`] ? 'is-invalid' : ''}`}
                      value={addr.cityId}
                      onChange={(e) =>
                        updateAddressField(idx, 'cityId', e.target.value)
                      }
                      disabled={!addr.countryId}
                    >
                      <option value="">
                        {addr.countryId ? '— Select City —' : '— Select Country first —'}
                      </option>
                      {addr.cities.map((city) => (
                        <option key={city.id} value={city.id}>
                          {city.name}
                        </option>
                      ))}
                    </select>
                    {errors[`addr_city_${idx}`] && (
                      <div className="invalid-feedback">{errors[`addr_city_${idx}`]}</div>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/*  Family Members  */}
        <div className="card mb-4">
          <div className="card-header bg-secondary text-white fw-semibold">
            Family Members
          </div>
          <div className="card-body">
            {/* Search */}
            <div className="mb-3 position-relative">
              <label htmlFor="family-search" className="form-label">
                Search customers by name:
              </label>
              <input
                id="family-search"
                type="text"
                className="form-control"
                placeholder="Type at least 2 characters..."
                value={familySearchQuery}
                onChange={(e) => handleFamilySearch(e.target.value)}
                autoComplete="off"
              />
              {familySearchResults.length > 0 && (
                <div
                  className="list-group position-absolute w-100 shadow-sm"
                  style={{ zIndex: 100, top: '100%' }}
                  id="family-search-results"
                >
                  {familySearchResults.map((member) => (
                    <button
                      key={member.id}
                      type="button"
                      id={`family-result-${member.id}`}
                      className="list-group-item list-group-item-action"
                      onClick={() => selectFamilyMember(member)}
                    >
                      <strong>{member.name}</strong>{' '}
                      <code className="text-muted">({member.nicNumber})</code>
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* Selected members */}
            {selectedFamilyMembers.length > 0 ? (
              <div>
                <p className="small text-muted mb-2">Selected family members:</p>
                <div className="d-flex flex-wrap gap-2" id="selected-family-members">
                  {selectedFamilyMembers.map((fm) => (
                    <span key={fm.id} className="badge bg-primary fs-6 py-2 px-3">
                      {fm.name}
                      <button
                        type="button"
                        id={`btn-remove-family-${fm.id}`}
                        className="btn-close btn-close-white ms-2"
                        style={{ fontSize: '0.6rem' }}
                        onClick={() => removeFamilyMember(fm.id)}
                        aria-label={`Remove ${fm.name}`}
                      />
                    </span>
                  ))}
                </div>
              </div>
            ) : (
              <p className="text-muted small mb-0">No family members selected.</p>
            )}
          </div>
        </div>

        {/*  Submit  */}
        <div className="d-flex gap-3">
          <button
            type="submit"
            id="btn-submit"
            className="btn btn-primary btn-lg px-5"
            disabled={loading}
          >
            {loading ? (
              <>
                <span className="spinner-border spinner-border-sm me-2" />
                Saving...
              </>
            ) : isEditMode ? (
              '💾 Update Customer'
            ) : (
              '✅ Create Customer'
            )}
          </button>
          <button
            type="button"
            id="btn-cancel"
            className="btn btn-outline-secondary btn-lg"
            onClick={() => navigate(isEditMode ? `/customers/${id}` : '/')}
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
};

export default CustomerFormPage;
