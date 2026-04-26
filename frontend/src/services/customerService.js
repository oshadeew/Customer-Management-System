import axios from 'axios';

const BASE_URL = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

//  Customer APIs

/*
  GET /api/customers?page=0&size=10
  Returns paginated customer list.
 */
export const getCustomers = (page = 0, size = 10) =>
  api.get(`/customers`, { params: { page, size } });

/*
  GET /api/customers/{id}
  Returns full customer details.
 */
export const getCustomerById = (id) =>
  api.get(`/customers/${id}`);

/*
  POST /api/customers
  Creates a new customer.
 */
export const createCustomer = (data) =>
  api.post(`/customers`, data);

/*
  PUT /api/customers/{id}
  Updates an existing customer.
 */
export const updateCustomer = (id, data) =>
  api.put(`/customers/${id}`, data);

/*
  GET /api/customers/search?name=xyz
  Searches customers by name
 */
export const searchCustomers = (name) =>
  api.get(`/customers/search`, { params: { name } });

/*
  POST /api/customers/bulk-upload
  Uploads an .xlsx file for bulk create/update.
 */
export const bulkUploadCustomers = (file) => {
  const formData = new FormData();
  formData.append('file', file);
  return api.post(`/customers/bulk-upload`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
};

// Country & City APIs

/*
  GET /api/countries
  Returns all countries.
 */
export const getCountries = () =>
  api.get(`/countries`);

/*
  GET /api/cities/{countryId}
  Returns cities for a given country.
 */
export const getCitiesByCountry = (countryId) =>
  api.get(`/cities/${countryId}`);
