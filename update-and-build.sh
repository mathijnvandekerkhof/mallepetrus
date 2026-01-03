#!/bin/bash

# JIPTV Update and Build Script
# Usage: ./update-and-build.sh

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

print_success() {
    echo -e "${GREEN}✅${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_error() {
    echo -e "${RED}❌${NC} $1"
}

print_step() {
    echo -e "${CYAN}▶${NC} $1"
}

# Function to get available branches (clean)
get_branches() {
    # First try to fetch quietly
    git fetch origin --quiet 2>/dev/null || git fetch origin 2>/dev/null || true
    
    # Get remote branches, clean them up
    git branch -r 2>/dev/null | \
        grep -v HEAD | \
        grep -v "origin/HEAD" | \
        sed 's/origin\///' | \
        sed 's/^[[:space:]]*//' | \
        sed 's/[[:space:]]*$//' | \
        sort | \
        uniq | \
        grep -v '^$'
}$'
}$'
}

# Function to show current status
show_status() {
    local current_branch=$(git branch --show-current)
    local last_commit=$(git log -1 --pretty=format:'%h - %s (%cr)' 2>/dev/null || echo "No commits")
    
    echo -e "${CYAN}📍 Current Status:${NC}"
    echo "   Repository: $(pwd)"
    echo "   Branch: ${current_branch}"
    echo "   Last commit: ${last_commit}"
    echo
}

# Function to select branch with numbered menu
select_branch() {
    local current_branch=$(git branch --show-current)
    
    # Get available branches into array
    local branches=()
    while IFS= read -r branch; do
        if [ -n "$branch" ]; then
            branches+=("$branch")
        fi
    done < <(get_branches)
    
    if [ ${#branches[@]} -eq 0 ]; then
        print_error "No remote branches found!"
        exit 1
    fi
    
    echo -e "${CYAN}▶ Branch Selection${NC}"
    echo "Available branches:"
    echo
    for i in "${!branches[@]}"; do
        local marker=""
        if [ "${branches[$i]}" = "$current_branch" ]; then
            marker=" ${GREEN}← current${NC}"
        fi
        printf "   ${BLUE}%2d)${NC} %-20s%s\n" $((i+1)) "${branches[$i]}" "$marker"
    done
    echo
    
    # Get user selection
    while true; do
        echo -n "Select branch by number (1-${#branches[@]}) or press Enter for current: "
        read selection
        
        # If empty, use current branch
        if [ -z "$selection" ]; then
            echo "$current_branch"
            return
        fi
        
        # Validate number
        if [[ "$selection" =~ ^[0-9]+$ ]] && [ "$selection" -ge 1 ] && [ "$selection" -le ${#branches[@]} ]; then
            echo "${branches[$((selection-1))]}"
            return
        else
            print_error "Invalid selection. Please enter a number between 1 and ${#branches[@]}"
        fi
    done
}

# Function to update repository
update_repository() {
    local branch=$1
    local current_branch=$(git branch --show-current)
    
    print_step "Updating repository to branch: ${branch}"
    
    # Fetch latest changes
    print_info "Fetching latest changes..."
    if ! git fetch origin; then
        print_error "Failed to fetch from origin"
        exit 1
    fi
    
    # Check if branch exists on remote
    if ! git show-ref --verify --quiet refs/remotes/origin/$branch; then
        print_error "Branch '$branch' does not exist on remote!"
        exit 1
    fi
    
    # Reset any local changes first
    print_info "Resetting local changes..."
    if ! git reset --hard HEAD; then
        print_error "Failed to reset local changes"
        exit 1
    fi
    
    # Switch to branch if needed
    if [ "$current_branch" != "$branch" ]; then
        print_info "Switching to branch: $branch"
        if ! git checkout $branch; then
            print_error "Failed to switch to branch: $branch"
            exit 1
        fi
    fi
    
    # Force pull latest changes (reset to remote)
    print_info "Pulling latest changes (forced update)..."
    if ! git reset --hard origin/$branch; then
        print_error "Failed to pull changes"
        exit 1
    fi
    
    print_success "Repository updated successfully!"
}

# Function to build Docker image
build_docker_image() {
    print_step "Building Docker image: jiptv:latest"
    
    # Check if Dockerfile exists
    if [ ! -f "Dockerfile" ]; then
        print_error "Dockerfile not found in current directory!"
        exit 1
    fi
    
    # Build the image
    print_info "Starting Docker build process..."
    if docker build -t jiptv:latest .; then
        print_success "Docker image built successfully!"
        
        # Show image info
        echo
        print_info "Image details:"
        docker images jiptv:latest --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}" | head -2
    else
        print_error "Docker build failed!"
        exit 1
    fi
}

# Function to show restart instructions
show_restart_instructions() {
    echo
    echo -e "${CYAN}🔄 Next Steps:${NC}"
    echo "   1. Go to Portainer: https://dock.mallepetrus.nl"
    echo "   2. Navigate to Stacks → jiptv-app"
    echo "   3. Click 'Restart' to use the new image"
    echo
    print_warning "The new image will be used after restart!"
}

# Main script execution
main() {
    clear
    echo "╔══════════════════════════════════════════════════════════════╗"
    echo "║                🚀 JIPTV Update & Build Script                ║"
    echo "╚══════════════════════════════════════════════════════════════╝"
    echo
    
    # Check if we're in a git repository
    if ! git rev-parse --git-dir > /dev/null 2>&1; then
        print_error "Not in a git repository!"
        exit 1
    fi
    
    # Check if Docker is available
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed or not in PATH!"
        exit 1
    fi
    
    # Show current status
    show_status
    
    # Select branch
    selected_branch=$(select_branch)
    
    # Show summary
    echo
    echo -e "${CYAN}📋 Summary:${NC}"
    echo "   Selected branch: ${selected_branch}"
    echo "   Action: Update code and build Docker image"
    echo -e "   ${YELLOW}⚠ Warning: Any local changes will be discarded!${NC}"
    echo
    
    # Confirm action
    read -p "Continue? (y/N): " confirm
    if [[ ! $confirm =~ ^[Yy]$ ]]; then
        print_info "Operation cancelled."
        exit 0
    fi
    
    echo
    print_step "Starting update and build process..."
    echo
    
    # Update repository
    update_repository "$selected_branch"
    echo
    
    # Build Docker image
    build_docker_image
    
    # Show restart instructions
    show_restart_instructions
    
    print_success "Update and build completed successfully! 🎉"
}

# Run main function
main "$@"