# Simple HTML Admin Dashboard
FROM nginx:alpine

# Copy HTML file
COPY index.html /usr/share/nginx/html/

# Copy nginx config
COPY nginx.conf /etc/nginx/nginx.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]