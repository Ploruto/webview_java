(function() {
    if (window.Bridge) return;

    const __bridgeInternal = window.__bridgeInternal;
    delete window.__bridgeInternal; // Hide internal function

    let objectRegistry = {};
    let eventListeners = {}; // Event listener registry

    const Bridge = {
        /**
         * Register an event listener
         * @param {string} eventType - The event type to listen for
         * @param {function} callback - The callback function to invoke
         * @returns {function} Unsubscribe function
         */
        on(eventType, callback) {
            if (typeof eventType !== 'string' || typeof callback !== 'function') {
                throw new Error('Bridge.on() requires a string event type and callback function');
            }
            
            if (!eventListeners[eventType]) {
                eventListeners[eventType] = [];
            }
            
            eventListeners[eventType].push(callback);
            
            // Return unsubscribe function
            return () => {
                const index = eventListeners[eventType].indexOf(callback);
                if (index > -1) {
                    eventListeners[eventType].splice(index, 1);
                }
            };
        },

        /**
         * Register a one-time event listener
         * @param {string} eventType - The event type to listen for
         * @param {function} callback - The callback function to invoke
         */
        once(eventType, callback) {
            const unsubscribe = Bridge.on(eventType, (data) => {
                unsubscribe();
                callback(data);
            });
            return unsubscribe;
        },

        /**
         * Remove event listener(s)
         * @param {string} eventType - The event type
         * @param {function} [callback] - Optional specific callback to remove
         */
        off(eventType, callback) {
            if (!eventListeners[eventType]) return;
            
            if (callback) {
                const index = eventListeners[eventType].indexOf(callback);
                if (index > -1) {
                    eventListeners[eventType].splice(index, 1);
                }
            } else {
                // Remove all listeners for this event type
                delete eventListeners[eventType];
            }
        },

        __internal: {
            sendMessageToJava(type, data) {
                return __bridgeInternal(type, data);
            },

            /**
             * Dispatch an event from Java
             * Called by Java's bridge.emit()
             */
            dispatch(eventType, data) {
                console.log('[Bridge] Event dispatched:', eventType, data);
                
                const listeners = eventListeners[eventType];
                if (listeners && listeners.length > 0) {
                    listeners.forEach(callback => {
                        try {
                            callback(data);
                        } catch (error) {
                            console.error('[Bridge] Error in event listener for', eventType, ':', error);
                        }
                    });
                }
            },

            /**
             * Update property cache directly (called by propertyUpdated event)
             * @param {string} objectId - The object ID
             * @param {string} propertyName - The property name
             * @param {*} value - The new value
             */
            updatePropertyCache(objectId, propertyName, value) {
                const obj = objectRegistry[objectId];
                if (obj && obj.__internal && obj.__internal.propertyCache) {
                    obj.__internal.propertyCache[propertyName] = value;
                    console.log('[Bridge] Property cache updated:', objectId, propertyName, '=', value);
                }
            },

            defineObject(path, id) {
                const parts = path.split('.');
                const propertyName = parts.pop();

                let proxy;
                const propertyCache = {}; // Cache for property values

                const object = {
                    __internal: {
                        id: id,
                        propertyCache: propertyCache,

                        defineFunction(name) {
                            object[name] = async function() {
                                return await Bridge.__internal.invoke(
                                    id,
                                    name,
                                    Array.from(arguments)
                                );
                            };
                        },

                        defineProperty(name) {
                            // Initialize property in cache
                            propertyCache[name] = undefined;
                            
                            Object.defineProperty(object, name, {
                                get() {
                                    // Trigger async fetch to update cache
                                    Bridge.__internal.get(id, name).then(value => {
                                        propertyCache[name] = value;
                                    });
                                    // Return current cached value immediately
                                    return propertyCache[name];
                                },
                                set(value) {
                                    propertyCache[name] = value;
                                    Bridge.__internal.set(id, name, value);
                                },
                                enumerable: true,
                                configurable: true
                            });
                            
                            // Fetch initial value
                            Bridge.__internal.get(id, name).then(value => {
                                propertyCache[name] = value;
                            });
                        }
                    }
                };

                const handler = {
                    get(obj, property) {
                        // If property exists on object, return it
                        if (property in obj) {
                            return obj[property];
                        }
                        
                        // For dynamic properties, trigger fetch and return cached value
                        Bridge.__internal.get(id, property).then(value => {
                            propertyCache[property] = value;
                        });
                        
                        return propertyCache[property];
                    },
                    set(obj, property, value) {
                        propertyCache[property] = value;
                        Bridge.__internal.set(id, property, value);
                        return true;
                    }
                };

                Object.freeze(object.__internal);
                proxy = new Proxy(object, handler);

                // Resolve the root object
                let root = window;
                for (const part of parts) {
                    root = root[part];
                }

                Object.defineProperty(root, propertyName, {
                    value: proxy,
                    writable: true,
                    configurable: true
                });

                objectRegistry[id] = proxy;
            },

            async get(id, property) {
                return await Bridge.__internal.sendMessageToJava('GET', { id, property });
            },

            async set(id, property, newValue) {
                return await Bridge.__internal.sendMessageToJava('SET', {
                    id,
                    property,
                    newValue
                });
            },

            async invoke(id, func, arguments) {
                return await Bridge.__internal.sendMessageToJava('INVOKE', {
                    id,
                    function: func,
                    arguments
                });
            }
        }
    };

    // Auto-subscribe to propertyUpdated event for cache synchronization
    Bridge.on('propertyUpdated', (data) => {
        // data = { objectId: 'xxx', property: 'count', value: 5 }
        if (data && data.objectId && data.property !== undefined) {
            Bridge.__internal.updatePropertyCache(data.objectId, data.property, data.value);
        }
    });

    Object.freeze(Bridge);
    Object.freeze(Bridge.__internal);
    Object.defineProperty(window, 'Bridge', {
        value: Bridge,
        writable: false,
        configurable: false
    });

    console.log('[Bridge] Initialized with event system and property cache sync');
})();
